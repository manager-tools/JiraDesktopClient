package com.almworks.util.cache;



import org.almworks.util.Collections15;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public abstract class CachedObject <K> extends Keyed<K> {
  /**
   * Weight for the cache. Objects with lower weight access removed first.
   * This field is package-visible, but other classes may do only reading!
   * To modify this field, use methods of this class.
   */
  volatile int myWeight;
  private final Object myWeightLock = new Object();
  private final List<WeakReference<CachedObjectProxy>> myProxies = Collections15.arrayList();
  private long myLastTimeProxiesSwept = 0;
  private static final long MIN_SWEEP_INTERVAL = 30000;

  public CachedObject(K key) {
    super(key);
  }

  final void accessed() {
    synchronized (myWeightLock) {
      myWeight++;
    }
  }

  final void degradeWeight() {
    synchronized (myWeightLock) {
      myWeight /= 2;
    }
  }

  final void setWeight(int weight) {
    synchronized (myWeightLock) {
      myWeight = weight;
    }
  }

  final void onUnload() {
    synchronized (myProxies) {
      for (Iterator<WeakReference<CachedObjectProxy>> iterator = myProxies.iterator(); iterator.hasNext();) {
        CachedObjectProxy proxy = iterator.next().get();
        if (proxy != null)
          proxy.clearDelegate();
      }
      myProxies.clear();
    }
  }

  final void couple(CachedObjectProxy proxy) {
    synchronized (myProxies) {
      long time = System.currentTimeMillis();
      if (time - myLastTimeProxiesSwept > MIN_SWEEP_INTERVAL) {
        for (Iterator<WeakReference<CachedObjectProxy>> iterator = myProxies.iterator(); iterator.hasNext();)
          if (iterator.next().get() == null)
            iterator.remove();
        myLastTimeProxiesSwept = time;
      }
      myProxies.add(new WeakReference<CachedObjectProxy>(proxy));
    }
  }
}
