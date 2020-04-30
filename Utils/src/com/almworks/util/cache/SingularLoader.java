package com.almworks.util.cache;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public class SingularLoader <K, T extends CachedObject<K>> implements CachedObjectLoader<K, T>, Cache<K, T> {
  private final T myObject;

  public SingularLoader(T value) {
    assert value != null;
    myObject = value;
  }

  public T loadObject(K key) {
    assert key.equals(myObject.key());
    return myObject;
  }

  public T get(K key, CachedObjectProxy<K, T> cachedObjectProxy) {
    return loadObject(key);
  }

  public void invalidate(K key) {
    throw new UnsupportedOperationException();
  }

  public static <K, T extends CachedObject<K>> SingularLoader<K, T> create(K key, T value) {
    assert key.equals(value.key());
    return new SingularLoader<K, T>(value);
  }
}
