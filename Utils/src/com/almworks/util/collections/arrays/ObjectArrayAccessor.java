package com.almworks.util.collections.arrays;

import org.jetbrains.annotations.Nullable;

public class ObjectArrayAccessor implements NullableArrayStorageAccessor {
  public static final ObjectArrayAccessor INSTANCE = new ObjectArrayAccessor();

  public Object copyValue(Object src, int srcIndex, Object dst, int dstIndex) {
    Object value = getObjectValue(src, srcIndex);
    return setObjectValue(dst, dstIndex, value);
  }

  public Object getObjectValue(Object storage, int index) {
    Object[] array = getArray(storage);
    return array != null && index < array.length ? array[index] : null;
  }

  @Override
  public Object shiftLeft(Object storage, int offset, int count) {
    if (count <= 0) return storage;
    Object[] array = getArray(storage);
    if (array != null) {
      int copyCount = array.length - offset - count;
      if (copyCount > 0) {
        System.arraycopy(array, offset + count, array, offset, copyCount);
      }
      for (int i = array.length - count; i < array.length; i++) {
        array[i] = null;
      }
    }
    return array;
  }

  @Override
  public Object shiftRight(Object storage, int offset, int count) {
    if (count <= 0) return storage;
    Object[] array = getArray(storage);
    if (array != null) {
      int len = array.length;
      // no realloc at all if inserting items after the end of array
      if (offset >= len) return array;
      array = getOrReallocArray(storage, len + count - 1);
      int copyCount = len - offset;
      if (copyCount > 0) {
        System.arraycopy(array, offset, array, offset + count, copyCount);
      }
      for (int i = 0; i < count; i++) {
        array[offset + i] = null;
      }
    }
    return array;
  }

  public Object setNull(Object storage, int index) {
    return setObjectValue(storage, index, null);
  }

  @Override
  public boolean isNull(Object storage, int index) {
    return getObjectValue(storage, index) == null;
  }

  public Object setObjectValue(Object storage, int index, Object value) {
    Object[] array;
    if (value != null) {
      array = getOrReallocArray(storage, index);
    } else {
      array = getArray(storage);
      if (array == null || index >= array.length)
        return storage;
    }
    array[index] = value;
    return array;
  }

  @Nullable
  private static Object[] getArray(Object storage) {
    if (!(storage instanceof Object[]))
      return null;
    return (Object[]) storage;
  }

  private static Object[] getOrReallocArray(Object storage, int requiredIndex) {
    if (!(storage instanceof Object[]))
      return reallocArray(null, requiredIndex);
    Object[] array = (Object[]) storage;
    return array.length > requiredIndex ? array : reallocArray(array, requiredIndex);
  }

  private static Object[] reallocArray(Object[] array, int requiredIndex) {
    assert array == null || array.length <= requiredIndex;
    Object[] newArray = new Object[requiredIndex + 1];
    if (array != null)
      System.arraycopy(array, 0, newArray, 0, array.length);
    return newArray;
  }
}
