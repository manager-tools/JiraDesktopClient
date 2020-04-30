package com.almworks.util.cache;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface Cache <K, T extends CachedObject<K>> {
  T get(K key, CachedObjectProxy<K, T> requestor);

  void invalidate(K key);
}
