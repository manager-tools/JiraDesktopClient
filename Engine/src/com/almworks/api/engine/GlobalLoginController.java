package com.almworks.api.engine;

import com.almworks.util.LocalLog;
import com.almworks.util.Pair;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Computable;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GlobalLoginController implements Startable {
  public static final Role<GlobalLoginController> ROLE = Role.role("loginController", GlobalLoginController.class);
  private static final LocalLog log = LocalLog.topLevel("GlobalLogin");
  private static final String FAILED_SITES = "invalid";

  private final Set<String> myFailedSites = Collections15.hashSet();
  private final Configuration myConfig;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public GlobalLoginController(Configuration config) {
    myConfig = config;
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  @Override
  public void start() {
    List<String> failures = myConfig.getAllSettings(FAILED_SITES);
    synchronized (myFailedSites) {
      myFailedSites.addAll(failures);
    }
    for (String failure : failures) log.warning("Loaded login failure:", failure);
    if (!failures.isEmpty()) myModifiable.fireChanged();
  }

  @Override
  public void stop() {
  }

  private final Object myUpdateLock = new Object();
  private Pair<Computable<?>, Thread> myCurrentTask;
  private final List<Connection> myLockedConnections = new ArrayList<>();

  /**
   * Implements an application-wide lock for restore/update credentials user interactions. This lock allows only one connection
   * to interact with user at the same time. This way connection may safely use modal windows without interference with other
   * connections.
   * @param connection the connection that needs to interact with user to restore/update credentials
   * @param computable the interaction that obtains updated credentials.
   * @param <T> type of user interaction result
   * @return result of user interaction provided by the computable parameter.<br>
   *   null if no user interaction actually performed
   */
  public <T> T updateLogin(Connection connection, Computable<T> computable) {
    synchronized (myUpdateLock) {
      if (myLockedConnections.contains(connection)) return null;
      myLockedConnections.add(connection);
    }
    try {
      long start = System.currentTimeMillis();
      while (true) {
        synchronized (myUpdateLock) {
          if (myCurrentTask != null) {
            try {
              log.debug("Blocked", System.currentTimeMillis() - start, Thread.currentThread(), computable, " running:", myCurrentTask);
              myUpdateLock.wait(1000);
            } catch (InterruptedException e) {
              throw new RuntimeInterruptedException(e);
            }
            continue;
          }
          myCurrentTask = Pair.create(computable, Thread.currentThread());
          log.debug("Starting", myCurrentTask);
          break;
        }
      }
      try {
        return computable.compute();
      } finally {
        synchronized (myUpdateLock) {
          log.debug("Complete", myCurrentTask);
          myCurrentTask = null;
          myUpdateLock.notify();
        }
      }
    } finally {
      synchronized (myUpdateLock) {
        myLockedConnections.remove(connection);
      }
    }
  }
}
