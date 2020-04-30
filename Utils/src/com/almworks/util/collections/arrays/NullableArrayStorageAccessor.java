package com.almworks.util.collections.arrays;

public interface NullableArrayStorageAccessor extends ArrayStorageAccessor {
  boolean isNull(Object storage, int index);

  Object setNull(Object storage, int index);
}
