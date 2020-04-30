package com.almworks.timetrack.uam;

import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.timetrack.api.UserActivityMonitor;
import com.almworks.util.Env;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.PersistableLong;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserActivityMonitorImpl implements UserActivityMonitor, Startable {
  private static boolean DISABLE_NATIVE_ACTIVITY_PROVIDER = Env.getBoolean("disable.native.uam");

  private long myLastUserActivity;
  private long myPingNanos;
  private long myPingTime;
  private long myStartTime;
  private long myLastTimePingSaved;
  private OutagePeriod myLastOutagePeriod;
  private Timer myTimer;

  private LastUserActivityProvider myLastUserActivityProvider;

  private final Store myStore;
  private static final int STEP = 1000;
  private final PersistableLong myPersister = new PersistableLong();

  public UserActivityMonitorImpl(Store store) {
    myStore = store;
    myLastUserActivityProvider = selectProvider();
    Log.debug("UAM: using " + myLastUserActivityProvider);
  }

  private static LastUserActivityProvider selectProvider() {
    if (DISABLE_NATIVE_ACTIVITY_PROVIDER)
      return new DefaultCrossPlatformLastUserActivityProvider();
    if (Env.isWindows())
      return new WindowsLastUserActivityProvider();
    return new DefaultCrossPlatformLastUserActivityProvider();
  }

  public void start() {
    if (StoreUtils.restorePersistable(myStore, "*", myPersister)) {
      Long v = myPersister.access();
      synchronized (this) {
        myPingTime = v == null ? 0 : v;
        myStartTime = System.currentTimeMillis();
        myLastUserActivity = myStartTime;
        myLastTimePingSaved = myStartTime;
        if (myPingTime > 0 && (myStartTime - myPingTime) < 2 * Const.MINUTE) {
          setLastOutagePeriod(new OutagePeriod(myPingTime, myStartTime, true));
        }
      }
    }
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        setLastUserActivityTime(System.currentTimeMillis());
        myTimer = new Timer(STEP, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ping();
          }
        });
        myTimer.start();
      }
    });
  }

  public void stop() {
  }

  private void ping() {
    pingUserTime();
    pingOutagePeriod();
  }

  private void pingOutagePeriod() {
    long pingTime = System.currentTimeMillis();
    long pingNanos = System.nanoTime();
    synchronized (this) {
      if (myPingNanos > 0 && myPingNanos < pingNanos && (pingNanos - myPingNanos) > STEP * 20 &&
        myPingTime < pingTime)
      {
        setLastOutagePeriod(new OutagePeriod(myPingTime, pingTime, false));
      }
      myPingNanos = pingNanos;
      myPingTime = pingTime;
    }
    if (myLastTimePingSaved > pingTime || (pingTime - myLastTimePingSaved) > Const.MINUTE) {
      myLastTimePingSaved = pingTime;
      ThreadGate.LONG.execute(new Runnable() {
        public void run() {
          synchronized (UserActivityMonitorImpl.this) {
            myPersister.set(myPingTime);
          }
          StoreUtils.storePersistable(myStore, "*", myPersister);
        }
      });
    }
  }

  private void pingUserTime() {
    long t = myLastUserActivityProvider.getLastUserActivityTime();
    // for stability, notice only 1-second differences
    if (Math.abs(t - myLastUserActivity) > Const.SECOND) {
      myLastUserActivity = t;
    }
  }

  public synchronized OutagePeriod getLastOutagePeriod() {
    return myLastOutagePeriod;
  }

  public synchronized long getLastUserActivityTime() {
    return myLastUserActivity;
  }

  private synchronized void setLastUserActivityTime(long time) {
    myLastUserActivity = time;
  }

  private synchronized void setLastOutagePeriod(OutagePeriod period) {
    myLastOutagePeriod = period;
  }
}
