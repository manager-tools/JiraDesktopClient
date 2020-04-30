package com.almworks.util.cache2;

import com.almworks.util.collections.QuickLargeList;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.Synchronized;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class LRULongCache<T extends LongKeyedCachedObject> {
  private static final int INITIAL_WEIGHT = 100;
  private static final Object NULL_SENTINEL = new Object();

  private final LongKeyedCachedObjectLoader<T> myLoader;

//  private final DebugDichotomy myHitMiss = new DebugDichotomy("hits", "misses", 10000);
//  private final SimplifiedFastMap<Long, Reference<T>> myInstanceCache;

  // index is atom id
  private final QuickLargeList<Reference<T>> myInstanceCache;
  private final Map<Long, Synchronized> myWaitObjects = new HashMap<Long, Synchronized>();

  private final Object myMapLock = new Object();
  private final ReferenceQueue<T> myReferenceQueue = new ReferenceQueue<T>();
  //private int myReentrancyLock = 0;

  private Thread myStatsThread;

  private volatile int myHits = 0;
  private volatile int myMisses = 0;

  private int mySofts = 0;
  private int myWeaks = 0;

  public LRULongCache(LongKeyedCachedObjectLoader<T> loader) {
    if (loader == null)
      throw new NullPointerException("loader");
    myLoader = loader;
    myInstanceCache = new QuickLargeList<Reference<T>>(15, true);
  }

  public static <T extends LongKeyedCachedObject> LRULongCache<T> create(LongKeyedCachedObjectLoader<T> loader) {
    return new LRULongCache<T>(loader);
  }

  public T get(long key) {
    Synchronized waitObject = null;
    boolean weAreLoading = false;

    Reference<T> qref = myInstanceCache.get((int) key);
    if (qref != null) {
      T instance = qref.get();
      if (instance != null) {
        myHits++; // may lose precision without sync, so what
        return instance;
      }
    }

    synchronized (myMapLock) {
      Reference<T> ref = myInstanceCache.get((int) key);
      if (ref != null) {
        T instance = ref.get();
        if (instance != null) {
          myHits++;
//    myHitMiss.a();
          return instance;
        } else {
          myInstanceCache.set((int) key, null);
          boolean isSoft = ref instanceof LRULongCache.KeyedSoftReference;
          removed(isSoft);
        }
      }
      // load object now
      myMisses++;
//    myHitMiss.b();
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

  private final void removed(boolean soft) {
    if (soft)
      mySofts--;
    else
      myWeaks--;
  }

  public void invalidate(long key) {
    synchronized (myMapLock) {
      Synchronized wait = myWaitObjects.get(key);
      if (wait == null) {
        // todo check if element is OK to remove
        Reference<T> ref = myInstanceCache.set((int) key, null);
        if (ref != null)
          removed(ref instanceof LRULongCache.KeyedSoftReference);
      }
      // todo what if somebody loading anyway?
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

  private void expungeStaleReferences() {
    Reference ref = myReferenceQueue.poll();
    if (ref == null)
      return;
    synchronized (myMapLock) {
      while (ref != null) {
        boolean soft = ref instanceof LRULongCache.KeyedSoftReference;
        long key = soft ? ((LRULongCache.KeyedSoftReference) ref).myKey : ((LRULongCache.KeyedWeakReference) ref).myKey;
        Reference<T> removed = myInstanceCache.set((int) key, null);
        if (removed != null) {
          if (removed != ref) {
            myInstanceCache.set((int) key, removed);
          } else {
            removed(soft);
          }
        }
        ref = myReferenceQueue.poll();
      }
//System.out.println("map size " + myInstanceCache.size());
    }
  }

  private T load(long key, Synchronized waitObject) {
    T instance = null;
    try {
      instance = myLoader.loadObject(key);
      instance.setWeight(INITIAL_WEIGHT);
      synchronized (myMapLock) {
        assert myInstanceCache.get((int) key) == null;
        expungeStaleReferences();
        LRULongCache.KeyedSoftReference ref = new LRULongCache.KeyedSoftReference(key, instance, myReferenceQueue);
        myInstanceCache.set((int) key, ref);
        mySofts++;
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

  private static final class KeyedSoftReference<T> extends SoftReference<T> {
    public final long myKey;

    public KeyedSoftReference(long key, T instance, ReferenceQueue<T> referenceQueue) {
      super(instance, referenceQueue);
      myKey = key;
    }
  }

  private static final class KeyedWeakReference<T> extends WeakReference<T> {
    public final long myKey;

    public KeyedWeakReference(long key, T instance, ReferenceQueue<T> referenceQueue) {
      super(instance, referenceQueue);
      myKey = key;
    }
  }
}
