package com.almworks.util.cache2;

public interface Cache <K, T extends CachedObject<K>> {
  T get(K key);
         
  void invalidate(K key);
}
