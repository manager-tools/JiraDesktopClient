package com.almworks.util.collections.arrays;

import com.almworks.integers.LongArray;
import org.almworks.util.Log;

public class LongArrayAccessor implements PrimitiveLongArrayAccessor {
  public static final LongArrayAccessor LONG_ARRAY = new LongArrayAccessor();

  @Override
  public Object setLongValue(Object storage, int index, long value) {
    LongArray array = array(storage, true);
    int sz = array.size();
    if (index >= sz) {
      if (value == 0) return array;
      array.insertMultiple(sz, 0, index - sz + 1);
    }
    array.set(index, value);
    return array;
  }

  @Override
  public long getLongValue(Object storage, int index) {
    LongArray array = array(storage, false);
    return array != null && index < array.size() ? array.get(index) : 0;
  }

  @Override
  public Object copyValue(Object src, int srcIndex, Object dst, int dstIndex) {
    return setLongValue(dst, dstIndex, getLongValue(src, srcIndex));
  }

  @Override
  public Object getObjectValue(Object storage, int index) {
    return getLongValue(storage, index);
  }

  @Override
  public Object setObjectValue(Object storage, int index, Object value) {
    long v = 0;
    if (value != null) {
      if (!(value instanceof Long)) {
        Log.warn(this + " cannot set long from " + value + " (" + value.getClass() + ")");
      } else {
        v = ((Long) value).longValue();
      }
    }
    return setLongValue(storage, index, v);
  }

  @Override
  public Object shiftLeft(Object storage, int offset, int count) {
    LongArray array = array(storage, false);
    if (array != null && offset < array.size()) {
      array.removeRange(offset, Math.min(array.size(), offset + count));
    }
    return array;
  }

  @Override
  public Object shiftRight(Object storage, int offset, int count) {
    LongArray array = array(storage, false);
    if (array != null && offset < array.size()) {
      array.insertMultiple(offset, 0, count);
    }
    return array;
  }

  private LongArray array(Object storage, boolean create) {
    if (storage instanceof LongArray)
      return (LongArray) storage;
    return create ? new LongArray() : null;
  }
}
