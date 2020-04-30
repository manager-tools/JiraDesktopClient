package com.almworks.util.cache2;

public interface CachedObjectLoader <K, T extends CachedObject<K>> {
  T loadObject(K key);
}
