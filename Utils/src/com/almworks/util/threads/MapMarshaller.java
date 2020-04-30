package com.almworks.util.threads;

import com.almworks.util.DECL;
import com.almworks.util.collections.FactoryWithParameter;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.util.Map;

/**
 * @author : Dyoma
 */
public class MapMarshaller<K, V> {
  private final Map<K, Object> myValues = Collections15.linkedHashMap();
  private final Object myLock = new Object();

  /**
   * @return null iff there is already value for the key or value was removed before computed. Otherwise returns value.compute()
   */
  @Nullable
  public V putNew(K key, Computable<V> value) {
    Lock<V> lock = null;
    try {
      synchronized (myLock) {
        Object v = myValues.get(key);
        Lock<?> currentLock = Util.castNullable(Lock.class, v);
        if (currentLock != null && currentLock.isCancelled()) {
          myValues.remove(key);
          v = null;
        }
        if (v != null) return null;
        lock = new Lock<V>();
        myValues.put(key, lock);
      }
      V v = value.compute();
      boolean success = putResult(key, v, lock);
      return success ? v : null;
    } finally {
      if (lock != null) lock.cancelBuilding();
    }
  }

  private boolean putResult(K key, V v, Lock<V> lock) {
    synchronized (myLock) {
      if (!lock.setValue(v))
        return false;
      Object old = myValues.get(key);
      if (old != lock)
        return false;
      myValues.put(key, v);
    }
    return true;
  }

  @Nullable
  public V getExisting(K key) {
    synchronized (myLock) {
      Object v = myValues.get(key);
      if (v == null || (v instanceof Lock))
        return null;
      return (V) v;
    }
  }

  @CanBlock
  @Nullable
  public V getBuilding(K key) throws InterruptedException {
    Threads.assertLongOperationsAllowed();
    Lock<V> buildLock;
    synchronized (myLock) {
      Object v = myValues.get(key);
      if (!(v instanceof Lock))
        return (V) v;
      buildLock = (Lock) v;
    }
    return buildLock.waitForCompletion();
  }

  /**
   * Removes value or stops computation (if value isn't computed yet)
   *
   * @return not null if value was already computed. null means the value wasn't computed yet.
   *         Wasn't every tried to compute or {@link #putNew(Object,Computable)} return null.
   */
  @Nullable
  public V clearExisting(K key) {
    DECL.assumeThreadMayBeAWT();
    Lock<V> buildLock;
    synchronized (myLock) {
      Object v = myValues.remove(key);
      if (!(v instanceof Lock<?>))
        return (V) v;
      buildLock = (Lock<V>) v;
    }
    buildLock.cancelBuilding();
    return null;
  }

  public void copyTo(MapMarshaller<K, V> other, FactoryWithParameter<V, V> copyValue) {
    synchronized (myLock) {
      Map<K, Object> otherValues = other.myValues;
      synchronized (other.myLock) {
        for (Map.Entry<K, Object> entry : myValues.entrySet()) {
          Object value = entry.getValue();
          Lock currentLock = Util.castNullable(Lock.class, value);
          if (currentLock != null && currentLock.isCancelled()) continue;
          assert !(value instanceof Lock<?>) : entry + " not implemented yet";
          otherValues.put(entry.getKey(), copyValue.create((V) value));
        }
      }
    }
  }

  public static <K, V> MapMarshaller<K, V> create() {
    return new MapMarshaller<K, V>();
  }


  private static final class Lock<V> extends SynchronizedBoolean {
    private V myValue;
    private boolean myCanceled = false;

    public Lock() {
      super(false);
    }

    public boolean setValue(V value) {
      synchronized (this) {
        myValue = value;
        set(true);
        return !myCanceled;
      }
    }

    @Nullable
    public V waitForCompletion() throws InterruptedException {
      waitForValue(true);
      synchronized (this) {
        return myValue;
      }
    }

    public void cancelBuilding() {
      if (get()) return;
      synchronized (this) {
        if (get()) return;
        myCanceled = true;
        set(true);
      }
    }

    protected Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    public boolean isCancelled() {
      synchronized (this) {
        return myCanceled;
      }
    }
  }
}
