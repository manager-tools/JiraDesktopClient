package com.almworks.util.cache;

import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.Heap;
import util.concurrent.Synchronized;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public class LRUCache <K, T extends CachedObject<K>> implements Cache<K, T> {
  private static final Object[] EMPTY = {};

  private final Map<K, T> myInstanceCache;
  private final Map<K, Synchronized<T>> myWaitObjects = Collections15.hashMap();
  private final CachedObjectLoader<K, T> myLoader;
  private final Object myMapLock = new Object();
  //private int myReentrancyLock = 0;

  private final int myCapacity;
  private final int myCapacityHigh;
  private final int myCapacityLow;
  private final Heap<T> myRemovalHeap;
  private final Comparator<T> myComparator;

  private volatile int myHits = 0;
  private volatile int myMisses = 0;
  private Thread myStatsThread;

  public LRUCache(CachedObjectLoader<K, T> loader, int capacity) {
    if (loader == null)
      throw new NullPointerException("loader");
    myLoader = loader;
    if (capacity < 10)
      capacity = 10;
    myCapacity = capacity;
    myCapacityHigh = capacity + capacity / 10;
    myCapacityLow = capacity - capacity / 10;
    myInstanceCache = new LinkedHashMap<K, T>(capacity + capacity / 3);

    myComparator = new Comparator<T>() {
      public int compare(T object1, T object2) {
        return Containers.compareInts(object2.myWeight, object1.myWeight);
      }
    };
    myRemovalHeap = new Heap<T>(myCapacityHigh - myCapacityLow, myComparator);
  }

  public T get(K key, CachedObjectProxy<K, T> requestor) {
    Synchronized<T> waitObject = null;
    boolean weAreLoading = false;
    synchronized (myMapLock) {
      T instance = myInstanceCache.get(key);
      if (instance != null) {
        hit();
        return instance;
      }
      // load object now
      miss();
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

  public void invalidate(K key) {
    synchronized (myMapLock) {
      Synchronized<T> wait = myWaitObjects.get(key);
      if (wait == null)
        removeElement(key);
      // todo what if somebody loading anyway?
    }
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

  private T load(K key, Synchronized<T> waitObject) {
    T instance = null;
    try {
      instance = myLoader.loadObject(key);
      instance.setWeight(100);
      Object[] removing = null;
      synchronized (myMapLock) {
        assert !myInstanceCache.containsKey(key);
        removing = checkSpace();
        myInstanceCache.put(key, instance);
        Synchronized<T> sync = myWaitObjects.remove(key);
        assert sync == waitObject;
      }
      for (int i = 0; i < removing.length; i++) {
        T removedInstance = (T) removing[i];
        removedInstance.onUnload();
      }
    } finally {
      boolean success = waitObject.commit(null, instance);
      assert success;
    }
    return instance;
  }

  private void miss() {
    myMisses++;
  }

  private void hit() {
    myHits++;
  }

  private Object[] checkSpace() {
    // called under myLock before addition of a new object
    int currentSize = myInstanceCache.size() + 1;
    if (currentSize < myCapacityHigh)
      return EMPTY;
    myRemovalHeap.clear();
    int heapSize = currentSize - myCapacityLow;
    myRemovalHeap.ensureCapacity(heapSize);
    for (Iterator<T> iterator = myInstanceCache.values().iterator(); iterator.hasNext();) {
      T instance = iterator.next();
      instance.degradeWeight();
      if (myRemovalHeap.size() < heapSize) {
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
      K key = instance.myKey;
      T wasInstance = removeElement(key);
      assert wasInstance == instance;
    }
    return removing;
  }

  private T removeElement(K key) {
    // todo check if element is OK to remove
    return myInstanceCache.remove(key);
  }

  public void runStats(final String statsName, final int period) {
    if (myStatsThread != null)
      return;
    myStatsThread = new Thread() {
      public void run() {
        try {
          while (!isInterrupted()) {
            Thread.sleep(period);
            int hits = LRUCache.this.myHits;
            int total = hits + LRUCache.this.myMisses;
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

  public static <K, T extends CachedObject<K>> LRUCache<K, T> create(int capacity, CachedObjectLoader<K, T> loader) {
    return new LRUCache<K, T>(loader, capacity);
  }

  public int getHits() {
    return myHits;
  }

  public int getMisses() {
    return myMisses;
  }
}
