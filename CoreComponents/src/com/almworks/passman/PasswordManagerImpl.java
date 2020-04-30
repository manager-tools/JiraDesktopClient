package com.almworks.passman;

import com.almworks.api.passman.PMCredentials;
import com.almworks.api.passman.PMDomain;
import com.almworks.api.passman.PasswordManager;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreAccess;
import com.almworks.api.store.StoreFeature;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.StorableException;
import com.almworks.util.properties.StorableMap;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;
import util.external.CompactInt;

import java.io.*;
import java.util.Iterator;
import java.util.List;

public class PasswordManagerImpl implements PasswordManager, Startable {
  private static final boolean STRICT_REALM_MATCHING = Env.getBoolean(GlobalProperties.STRICT_REALM_MATCHING);
  private static final int MARKER_DATAEND = 0;
  private static final int MARKER_CREDENTIALS = 1;

  static final String STORE_KEY = "X";

  private final Store myStore;
  private final List<Pair<PMDomain, PMCredentials>> myMemoryMap = Collections15.arrayList();
  private final List<Pair<PMDomain, PMCredentials>> myDiskImageMap = Collections15.arrayList();
  private final SynchronizedBoolean myDirty = new SynchronizedBoolean(false);
  private final SynchronizedBoolean myLoaded = new SynchronizedBoolean(false);
  private final Object myDiskWriteLock = new Object();

  public PasswordManagerImpl(Store store) {
    myStore = store;
  }

  public PMCredentials loadCredentials(PMDomain domain) {
    waitLoaded();
    synchronized (this) {
      PMCredentials result = getCredentials(domain, myMemoryMap);
      if (result != null)
        return result;
      result = getCredentials(domain, myDiskImageMap);
      return result;
    }
  }

  private PMCredentials getCredentials(PMDomain domain, List<Pair<PMDomain, PMCredentials>> map) {
    // strict match
    for (Pair<PMDomain, PMCredentials> pair : map) {
      if (Util.equals(pair.getFirst(), domain))
        return pair.getSecond();
    }

    // relaxed access
    String needsHost = PMDomain.HOST.get(domain.getMap());
    Integer needsPort = PMDomain.PORT.get(domain.getMap());
    if (needsPort == null || needsHost == null)
      return null;
    boolean any = PMDomain.REALM.get(domain.getMap()) == null || !STRICT_REALM_MATCHING;

    for (Pair<PMDomain, PMCredentials> pair : map) {
      StorableMap m = pair.getFirst().getMap();
      String host = PMDomain.HOST.get(m);
      Integer port = PMDomain.PORT.get(m);
      if (!needsHost.equals(host) || !needsPort.equals(port))
        continue;
      if (any)
        return pair.getSecond();
      String realm = PMDomain.REALM.get(m);
      if (realm == null) {
        // the default
        return pair.getSecond();
      }
    }

    return null;
  }

  public void saveCredentials(PMDomain domain, PMCredentials credentials, boolean saveOnDisk) {
    // save auth type for preliminary auth
    StorableMap map = credentials.getMap();
    if (PMCredentials.AUTHTYPE.get(map) == null) {
      String authtype = PMDomain.KIND.get(domain.getMap());
      if (authtype != null) {
        PMCredentials c = new PMCredentials();
        PMCredentials.USERNAME.put(c.getMap(), PMCredentials.USERNAME.get(map));
        PMCredentials.PASSWORD.put(c.getMap(), PMCredentials.PASSWORD.get(map));
        PMCredentials.AUTHTYPE.put(c.getMap(), authtype);
        c.getMap().fix();
        credentials = c;
      }
    }

    waitLoaded();
    synchronized (this) {
      if (saveOnDisk) {
        removeDomain(domain, myMemoryMap);
        removeDomain(domain, myDiskImageMap);
        myDiskImageMap.add(0, Pair.create(domain, credentials));
        setDirty();
      } else {
        removeDomain(domain, myMemoryMap);
        myMemoryMap.add(0, Pair.create(domain, credentials));
        if (removeDomain(domain, myDiskImageMap)) {
          setDirty();
        }
      }
    }
  }

  private boolean removeDomain(PMDomain domain, List<Pair<PMDomain, PMCredentials>> map) {
    boolean removed = false;
    for (Iterator<Pair<PMDomain, PMCredentials>> ii = map.iterator(); ii.hasNext();) {
      Pair<PMDomain, PMCredentials> pair = ii.next();
      if (Util.equals(pair.getFirst(), domain)) {
        ii.remove();
        removed = true;
      }
    }
    return removed;
  }

  private void setDirty() {
    myDirty.set(true);
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        write();
      }
    });
  }

  public void start() {
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        try {
          read();
        } finally {
          myLoaded.set(true);
        }
      }
    });
  }

  public void stop() {
  }

  private StoreAccess getAccess() {
    return myStore.access(STORE_KEY, StoreFeature.SECURE_STORE);
  }

  private synchronized void read() {
    StoreAccess storeAccess = getAccess();
    byte[] data;
    synchronized (myDiskWriteLock) {
      data = storeAccess.load();
    }
    if (data == null)
      return;
    DataInput in = new DataInputStream(new ByteArrayInputStream(data));
    myDiskImageMap.clear();
    try {
      while (true) {
        int marker = CompactInt.readInt(in);
        if (marker == MARKER_DATAEND)
          break;
        if (marker == MARKER_CREDENTIALS) {
          PMDomain domain = new PMDomain();
          domain.getMap().restore(in, PMDomain.HOST);
          PMCredentials credentials = new PMCredentials();
          credentials.getMap().restore(in, PMCredentials.PASSWORD);
          myDiskImageMap.add(Pair.create(domain, credentials));
          continue;
        }
        throw new StorableException("bad marker " + marker);
      }
    } catch (IOException e) {
      Log.warn("bad store format", e);
    } catch (StorableException e) {
      Log.warn("cached credentials may be lost", e);
    }
  }

  private void waitLoaded() {
    Threads.assertLongOperationsAllowed();
    try {
      myLoaded.waitForValue(true);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  private void write() {
    if (!myDirty.commit(true, false))
      return;

    List<Pair<PMDomain, PMCredentials>> map;
    synchronized (this) {
      map = Collections15.arrayList(myDiskImageMap);
    }

    synchronized (myDiskWriteLock) {
      StoreAccess storeAccess = getAccess();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(baos);
      try {
        for (Pair<PMDomain, PMCredentials> pair : map) {
          CompactInt.writeInt(out, MARKER_CREDENTIALS);
          pair.getFirst().getMap().store(out);
          pair.getSecond().getMap().store(out);
        }
        CompactInt.writeInt(out, MARKER_DATAEND);
        out.close();
        byte[] data = baos.toByteArray();
        storeAccess.store(data);
      } catch (IOException e) {
        Log.warn("unexpected exception", e);
      }
    }
  }
}
