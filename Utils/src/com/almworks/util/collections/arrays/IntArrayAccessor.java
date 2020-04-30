package com.almworks.util.collections.arrays;

import com.almworks.integers.IntArray;
import org.almworks.util.Log;

public class IntArrayAccessor implements PrimitiveIntArrayAccessor {
  public static final IntArrayAccessor INT_ARRAY = new IntArrayAccessor();

  @Override
  public Object setIntValue(Object storage, int index, int value) {
    IntArray array = array(storage, true);
    int sz = array.size();
    if (index >= sz) {
      if (value == 0)
        return array;
      array.insertMultiple(sz, 0, index - sz + 1);
    }
    array.set(index, value);
    return array;
  }

  @Override
  public int getIntValue(Object storage, int index) {
    IntArray array = array(storage, false);
    return array != null && index < array.size() ? array.get(index) : 0;
  }

  @Override
  public Object copyValue(Object src, int srcIndex, Object dst, int dstIndex) {
    return setIntValue(dst, dstIndex, getIntValue(src, srcIndex));
  }

  @Override
  public Object getObjectValue(Object storage, int index) {
    return getIntValue(storage, index);
  }

  @Override
  public Object setObjectValue(Object storage, int index, Object value) {
    int v = 0;
    if (value != null) {
      if (!(value instanceof Integer)) {
        Log.warn(this + " cannot set int from " + value + " (" + value.getClass() + ")");
      } else {
        v = (Integer) value;
      }
    }
    return setIntValue(storage, index, v);
  }

  @Override
  public Object shiftLeft(Object storage, int offset, int count) {
    IntArray array = array(storage, false);
    if (array != null && offset < array.size()) {
      array.removeRange(offset, Math.min(array.size(), offset + count));
    }
    return array;
  }

  @Override
  public Object shiftRight(Object storage, int offset, int count) {
    IntArray array = array(storage, false);
    if (array != null && offset < array.size()) {
      array.insertMultiple(offset, 0, count);
    }
    return array;
  }

  public int getArrayLength(Object storage) {
    IntArray intArray = array(storage, false);
    return intArray == null ? 0 : intArray.size();
  }

  private IntArray array(Object storage, boolean create) {
    if (storage instanceof IntArray)
      return (IntArray) storage;
    return create ? new IntArray() : null;
  }
}
