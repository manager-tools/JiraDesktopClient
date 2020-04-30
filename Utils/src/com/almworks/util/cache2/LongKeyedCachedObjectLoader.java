package com.almworks.util.cache2;

public interface LongKeyedCachedObjectLoader<T extends LongKeyedCachedObject> {
  T loadObject(long key);
}
