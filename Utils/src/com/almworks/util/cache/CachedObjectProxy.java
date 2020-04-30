package com.almworks.util.cache;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public abstract class CachedObjectProxy <K, T extends CachedObject<K>> extends Keyed<K> {
  private final Cache<K, T> myCache;
  private T myDelegate;

  protected CachedObjectProxy(Cache<K, T> cache, K key) {
    super(key);
    assert cache != null;
    myCache = cache;
  }

  // todo it's possible that while processing a method, the object gets removed from cache and another is loaded into
  // todo cache, so we have to guard against it
  protected final synchronized T delegate() {
    if (myDelegate == null) {
      myDelegate = myCache.get(myKey, this);
      myDelegate.couple(this);
    }
    myDelegate.accessed();
    return myDelegate;
  }

  synchronized final void clearDelegate() {
    myDelegate = null;
  }

  synchronized final boolean isDelegateAssigned() {
    return myDelegate != null;
  }
}
