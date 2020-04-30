package com.almworks.util.collections.arrays;

public interface PrimitiveIntArrayAccessor extends ArrayStorageAccessor {
  int getIntValue(Object storage, int index);

  Object setIntValue(Object storage, int index, int value);
}
