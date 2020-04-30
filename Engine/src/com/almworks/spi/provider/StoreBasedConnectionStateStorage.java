package com.almworks.spi.provider;

import com.almworks.api.connector.ConnectorStateStorage;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableMultiMap;
import com.almworks.util.io.persist.PersistableSerializable;
import com.almworks.util.io.persist.PersistableString;
import com.almworks.util.threads.Bottleneck;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.apache.commons.httpclient.Cookie;

import java.util.Map;

public class StoreBasedConnectionStateStorage implements ConnectorStateStorage {
  private final String myStoreId;
  private final Store myStore;

  private final Map<String, String> myMap = Collections15.hashMap();
  private final PersistableHashMap<String, String> myPersister =
    PersistableHashMap.create(new PersistableString(), new PersistableString());

  private MultiMap<String, Cookie> myCookies;
  private final PersistableMultiMap<String, Cookie> myCookiePersister =
    PersistableMultiMap.create(new PersistableString(), new PersistableSerializable(Cookie.class));

  private final Map<TypedKey<?>, ?> myRuntimeMap = Collections15.hashMap();

  private boolean myLoaded;

  private final Bottleneck myStoreBottleneck =
    new Bottleneck(500, ThreadGate.LONG(StoreBasedConnectionStateStorage.class), new Runnable() {
      public void run() {
        store();
      }
    });

  public StoreBasedConnectionStateStorage(Store store, String storeId) {
    myStore = store;
    myStoreId = storeId;
  }

  public void setPersistentLong(String key, long value) {
    setPersistentString(key, String.valueOf(value));
  }

  public long getPersistentLong(String key) {
    String s = getPersistentString(key);
    if (s == null)
      return 0;
    try {
      return Long.parseLong(s);
    } catch (Exception e) {
      return 0;
    }
  }

  public synchronized void setPersistentString(String key, String value) {
    ensureLoaded();
    myMap.put(key, value);
    myStoreBottleneck.request();
  }

  public synchronized String getPersistentString(String key) {
    ensureLoaded();
    String value = myMap.get(key);
    return value;
  }

  public synchronized void setCookies(MultiMap<String, Cookie> cookies) {
    ensureLoaded();
    myCookies = MultiMap.createCopyOrNull(cookies);
    myStoreBottleneck.request();
  }

  public synchronized MultiMap<String, Cookie> getCookies() {
    ensureLoaded();
    return MultiMap.createCopyOrNull(myCookies);
  }

  public synchronized void removePersistent(String key) {
    ensureLoaded();
    if (myMap.remove(key) != null) {
      myStoreBottleneck.request();
    }
  }

  public synchronized <T> void setRuntime(TypedKey<T> key, T value) {
    key.putTo(myRuntimeMap, value);
  }

  public synchronized <T> T getRuntime(TypedKey<T> key) {
    return key.getFrom(myRuntimeMap);
  }

  public synchronized void clearPersistent() {
    myLoaded = true;
    myMap.clear();
    myCookies = null;
    myStoreBottleneck.request();
  }

  private void ensureLoaded() {
    assert Thread.holdsLock(this);
    if (myLoaded)
      return;
    boolean success = StoreUtils.restorePersistable(myStore, myStoreId, myPersister);
    if (!success) {
      Log.debug("cannot restore connector state");
    } else {
      myMap.putAll(myPersister.access());
    }
    success = StoreUtils.restorePersistable(myStore, getCookiesId(), myCookiePersister);
    if (!success) {
      Log.debug("no cookies");
      myCookies = null;
    } else {
      myCookies = myCookiePersister.copy();
    }
    myLoaded = true;
  }


  private synchronized void store() {
    assert myLoaded;
    myPersister.set(myMap);
    StoreUtils.storePersistable(myStore, myStoreId, myPersister);
    String cookiesId = getCookiesId();
    if (myCookies == null) {
      myStore.access(cookiesId).clear();
    } else {
      myCookiePersister.set(myCookies);
      StoreUtils.storePersistable(myStore, cookiesId, myCookiePersister);
    }
  }

  private String getCookiesId() {
    return myStoreId + ".cookies";
  }
}
