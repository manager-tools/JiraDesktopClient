package com.almworks.util.cache2;

import com.almworks.util.collections.Containers;
import javolution.util.SimplifiedFastMap;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.Heap;
import util.concurrent.Synchronized;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public final class LRUCache2 <K, T extends CachedObject<K>> implements Cache<K, T> {
  private static final Object[] EMPTY = {};
  private static final int INITIAL_WEIGHT = 100;
  private final CachedObjectLoader<K, T> myLoader;
  private final Comparator<T> myComparator;

//  private final DebugDichotomy myHitMiss = new DebugDichotomy("hits", "misses", 10000);
  private final Heap<T> myRemovalHeap;

  private final SimplifiedFastMap<K, Reference<T>> myInstanceCache;
  private final Map<K, Synchronized<T>> myWaitObjects = new SimplifiedFastMap<K, Synchronized<T>>();
  private final Object myMapLock = new Object();
  private final ReferenceQueue<T> myReferenceQueue = new ReferenceQueue<T>();
  //private int myReentrancyLock = 0;

  private final int myCapacity;
  private final int myCapacityHigh;
  private final int myCapacityLow;
  private Thread myStatsThread;

  private volatile int myHits = 0;
  private volatile int myMisses = 0;

  private int mySofts = 0;
  private int myWeaks = 0;

  private final boolean myUnlimited;

  public LRUCache2(CachedObjectLoader<K, T> loader, int capacity) {
    if (loader == null)
      throw new NullPointerException("loader");
    myLoader = loader;
    myUnlimited = capacity == 0;

    myComparator = new Comparator<T>() {
      public int compare(T object1, T object2) {
        return Containers.compareInts(object2.myWeight, object1.myWeight);
      }
    };

    if (!myUnlimited) {
      if (capacity < 10)
        capacity = 10;
      myCapacity = capacity;
      myCapacityHigh = capacity + capacity / 10;
      myCapacityLow = capacity - capacity / 3;
      myInstanceCache = new SimplifiedFastMap<K, Reference<T>>(capacity + capacity / 3);
      myRemovalHeap = new Heap<T>(myCapacityHigh - myCapacityLow, myComparator);
    } else {
      myCapacity = 0;
      myCapacityHigh = 0;
      myCapacityLow = 0;
      myInstanceCache = new SimplifiedFastMap<K, Reference<T>>(1000);
      myRemovalHeap = null;
    }
    myInstanceCache.setShared(true);
  }

  public static <K, T extends CachedObject<K>> LRUCache2<K, T> create(int capacity, CachedObjectLoader<K, T> loader) {
    return new LRUCache2<K, T>(loader, capacity);
  }

  public T get(K key) {
    Synchronized<T> waitObject = null;
    boolean weAreLoading = false;

    Reference<T> qref = myInstanceCache.get(key);
    if (qref != null) {
      T instance = qref.get();
      if (instance != null) {
        myHits++; // may lose precision without sync, so what
        return instance;
      }
    }

    synchronized (myMapLock) {
      Reference<T> ref = myInstanceCache.get(key);
      if (ref != null) {
        T instance = ref.get();
        if (instance != null) {
          myHits++;
//    myHitMiss.a();
          return instance;
        } else {
          myInstanceCache.remove(key);
          boolean isSoft = ref instanceof LRUCache2.KeyedSoftReference;
          removed(isSoft);
        }
      }
      // load object now
      myMisses++;
//    myHitMiss.b();
      waitObject = myWaitObjects.get(key);
      if (waitObject == null) {
        waitObject = new Synchronized<T>(null);
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

/*
// debug version
  public T access(K key) {
Trace __miss;
Trace __all = Trace.trace("LRUCache2.access:ALL"); try {
Trace __wait1 = Trace.trace("LRUCache2.access:wait-1");
    Synchronized<T> waitObject = null;
    boolean weAreLoading = false;
    synchronized (myMapLock) {
Trace.endTrace(__wait1);
Trace __getFromCache = Trace.trace("LRUCache2.access:access-from-cache"); try {
      KeyedReference<K, T> ref = myInstanceCache.access(key);
      if (ref != null) {
        T instance = ref.access();
        if (instance != null) {
          hit();
          return instance;
        } else {
          myInstanceCache.remove(key);
          ref.removed();
        }
      }
} finally { Trace.endTrace(__getFromCache); }
__miss = Trace.trace("LRUCache2.access:miss");
      // load object now
      miss();
      waitObject = myWaitObjects.access(key);
      if (waitObject == null) {
        waitObject = new Synchronized<T>(null);
        myWaitObjects.put(key, waitObject);
        weAreLoading = true;
      }
    }
Trace.endTrace(__miss);

    assert waitObject != null;
    if (weAreLoading) {
Trace __load = Trace.trace("LRUCache2.access:load"); try {
      return load(key, waitObject);
} finally { Trace.endTrace(__load); }
    } else {
Trace __waitLoad = Trace.trace("LRUCache2.access:wait-load"); try {
      return waitLoad(waitObject);
} finally { Trace.endTrace(__waitLoad); }
    }
} finally { Trace.endTrace(__all); }
  }
*/

  public void invalidate(K key) {
    synchronized (myMapLock) {
      Synchronized<T> wait = myWaitObjects.get(key);
      if (wait == null) {
        // todo check if element is OK to remove
        Reference<T> ref = myInstanceCache.remove(key);
        if (ref != null)
          removed(ref instanceof LRUCache2.KeyedSoftReference);
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
              System.out.println(
                "[== cache(" + statsName + ") stats: hitRatio=" + result + "; accessCount=" + btotal + " ==]");
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

  private void checkSpace() {
    // called under myLock before addition of a new object
    // todo while this is happening, weight may change,  which ruin all sorting
    expungeStaleReferences();
    int currentSize = mySofts + 1;
    if (currentSize < myCapacityHigh)
      return;
    myRemovalHeap.clear();
    int removeCount = currentSize - myCapacityLow;
    myRemovalHeap.ensureCapacity(removeCount);
    for (Iterator<Reference<T>> ii = myInstanceCache.values().iterator(); ii.hasNext();) {
      if (removeCount <= 0)
        break;
      Reference<T> ref = ii.next();
      T instance = ref.get();
      boolean soft = ref instanceof LRUCache2.KeyedSoftReference;
      if (instance == null) {
        ii.remove();
        removed(soft);
        if (soft)
          removeCount--;
        continue;
      }
      if (soft)
        continue;
      instance.degradeWeight();
      if (myRemovalHeap.size() < removeCount) {
        myRemovalHeap.insert(instance);
      } else {
        T another = myRemovalHeap.peek();
        if (myComparator.compare(another, instance) < 0) {
          myRemovalHeap.extract();
          myRemovalHeap.insert(instance);
        }
      }
    }
    // now heap contains N least important instances
    Object[] removing = myRemovalHeap.copyUnsortedContents();
    myRemovalHeap.clear();
    for (int i = 0; i < removing.length; i++) {
      T instance = (T) removing[i];
      if (instance == null)
        continue;
      K key = instance.key();
      Reference<T> reference = myInstanceCache.remove(key);
      boolean soft = reference instanceof LRUCache2.KeyedSoftReference;
      assert soft : reference;
      removed(soft);
      KeyedWeakReference weak = new KeyedWeakReference(key, instance, myReferenceQueue);
      myInstanceCache.put(key, weak);
      myWeaks++;
    }
  }

  private void expungeStaleReferences() {
    Reference ref = myReferenceQueue.poll();
    if (ref == null)
      return;
    synchronized (myMapLock) {
      while (ref != null) {
        boolean soft = ref instanceof LRUCache2.KeyedSoftReference;
        Object key = soft ? ((KeyedSoftReference) ref).myKey : ((KeyedWeakReference) ref).myKey;
        Reference<T> removed = myInstanceCache.remove(key);
        if (removed != null) {
          if (removed != ref) {
            myInstanceCache.put((K) key, removed);
          } else {
            removed(soft);
          }
        }
        ref = myReferenceQueue.poll();
      }
//System.out.println("map size " + myInstanceCache.size());
    }
  }

  private T load(K key, Synchronized<T> waitObject) {
    T instance = null;
    try {
      instance = myLoader.loadObject(key);
      instance.setWeight(INITIAL_WEIGHT);
      synchronized (myMapLock) {
        assert !myInstanceCache.containsKey(key);
        if (!myUnlimited)
          checkSpace();
        else
          expungeStaleReferences();
        KeyedSoftReference ref = new KeyedSoftReference(key, instance, myReferenceQueue);
        myInstanceCache.put(key, ref);
        mySofts++;
        Synchronized<T> sync = myWaitObjects.remove(key);
        assert sync == waitObject;
      }
    } finally {
      boolean success = waitObject.commit(null, instance);
      assert success;
    }
    return instance;
  }

  private T waitLoad(Synchronized<T> waitObject) {
    try {
      waitObject.waitForNotNull();
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
    T instance = waitObject.get();
    assert instance != null;
    return instance;
  }

/*
  private static interface KeyedReference<K, T> {
    T get();
    K getKey();
    void inserted();
    void removed();
  }
*/



  private static final class KeyedSoftReference<K, T> extends SoftReference<T> /*implements KeyedReference<K, T>*/ {
    public final K myKey;

    public KeyedSoftReference(K key, T instance, ReferenceQueue<T> referenceQueue) {
      super(instance, referenceQueue);
      myKey = key;
    }
  }

  private static final class KeyedWeakReference<K, T> extends WeakReference<T> /*implements KeyedReference<K, T>*/ {
    public final K myKey;

    public KeyedWeakReference(K key, T instance, ReferenceQueue<T> referenceQueue) {
      super(instance, referenceQueue);
      myKey = key;
    }
  }
}
