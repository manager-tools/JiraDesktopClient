package com.almworks.util.collections.arrays;

public interface ArrayStorageAccessor {
  Object copyValue(Object src, int srcIndex, Object dst, int dstIndex);

  Object getObjectValue(Object storage, int index);

  Object setObjectValue(Object storage, int index, Object value);

  /**
   * inserts count undefined values at offset
   */
  Object shiftRight(Object storage, int offset, int count);

  /**
   * Removes count elements from the storage at offset
   */
  Object shiftLeft(Object storage, int offset, int count);
}
