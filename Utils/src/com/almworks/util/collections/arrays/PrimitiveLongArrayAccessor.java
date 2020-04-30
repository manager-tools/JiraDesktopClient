package com.almworks.util.collections.arrays;

public interface PrimitiveLongArrayAccessor extends ArrayStorageAccessor {
  Object setLongValue(Object storage, int index, long value);

  long getLongValue(Object storage, int index);
}
