package com.almworks.util.cache2;

import com.almworks.util.collections.QuickLargeList;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.Synchronized;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class NoCache<T extends LongKeyed> {
  private static final Object NULL_SENTINEL = new Object();

  private final LongKeyedLoader<T> myLoader;
  private final QuickLargeList<T> myInstanceCache;
  private final Map<Long, Synchronized> myWaitObjects = new HashMap<Long, Synchronized>();
  private final Object myMapLock = new Object();

  private Thread myStatsThread;

  private volatile int myHits = 0;
  private volatile int myMisses = 0;

  public NoCache(LongKeyedLoader<T> loader) {
    if (loader == null)
      throw new NullPointerException("loader");
    myLoader = loader;
    myInstanceCache = new QuickLargeList<T>(15, true);
  }

  public static <T extends LongKeyed> NoCache<T> create(LongKeyedLoader<T> loader) {
    return new NoCache<T>(loader);
  }

  public T get(long key) {
    Synchronized waitObject = null;
    boolean weAreLoading = false;

    T instance = myInstanceCache.get((int) key);
    if (instance != null) {
      myHits++; // may lose precision without sync, so what
      return instance;
    }

    synchronized (myMapLock) {
      instance = myInstanceCache.get((int) key);
      if (instance != null) {
        myHits++;
        return instance;
      }
      myMisses++;
      waitObject = myWaitObjects.get(key);
      if (waitObject == null) {
        waitObject = new Synchronized(null);
        myWaitObjects.put(key, waitObject);
        weAreLoading = true;
      }
    }

    assert waitObject != null;
    if (weAreLoading) {
      return load(key, waitObject);
    } else {
      return waitLoad(waitObject);
    }
  }

  public int getHits() {
    return myHits;
  }

  public int getMisses() {
    return myMisses;
  }

  public void runStats(final String statsName, final int period) {
    if (myStatsThread != null)
      return;
    myStatsThread = new Thread() {
      public void run() {
        try {
          while (!isInterrupted()) {
            Thread.sleep(period);
            int hits = myHits;
            int total = hits + myMisses;
            if (total > 0) {
              BigDecimal bhits = BigDecimal.valueOf(hits).setScale(2);
              BigDecimal btotal = BigDecimal.valueOf(total).setScale(0);
              BigDecimal result = bhits.multiply(BigDecimal.valueOf(100)).divide(btotal, 2, BigDecimal.ROUND_FLOOR);
              System.out
                .println("[== cache(" + statsName + ") stats: hitRatio=" + result + "; accessCount=" + btotal + " ==]");
            }
          }
        } catch (InterruptedException e) {
        }
      }
    };
    myStatsThread.setDaemon(true);
    myStatsThread.setName(getClass().getName() + ".statsThread");
    myStatsThread.start();
  }

  private T load(long key, Synchronized waitObject) {
    T instance = null;
    try {
      instance = myLoader.loadObject(key);
      synchronized (myMapLock) {
        assert myInstanceCache.get((int) key) == null;
        myInstanceCache.set((int) key, instance);
        Synchronized sync = myWaitObjects.remove(key);
        if (sync != waitObject) {
          assert false : key + " " + sync + " " + waitObject;
          Log.warn("incorrect wait object for " + key);
        }
      }
    } finally {
      boolean success = waitObject.commit(null, instance == null ? NULL_SENTINEL : instance);
      if (!success) {
        Log.warn("cannot set waitObject for " + key);
      }
    }
    return instance;
  }

  private T waitLoad(Synchronized waitObject) {
    try {
      waitObject.waitForNotNull();
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
    Object instance = waitObject.get();
    if (instance == NULL_SENTINEL)
      instance = null;
    return (T) instance;
  }

  public void invalidate(long key) {
    synchronized (myMapLock) {
      Synchronized wait = myWaitObjects.get(key);
      if (wait == null) {
        // todo check if element is OK to remove
        myInstanceCache.set((int) key, null);
      }
      // todo what if somebody loading anyway?
    }
  }
}
