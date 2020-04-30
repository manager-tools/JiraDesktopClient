package com.almworks.util.collections.arrays;

import util.external.BitSet2;

public class NullableLongArrayAccessor extends NullableArrayStorageDecorator implements PrimitiveLongArrayAccessor {
  public static final NullableLongArrayAccessor LONG_ARRAY_NULLABLE = new NullableLongArrayAccessor();

  public NullableLongArrayAccessor() {
    super(LongArrayAccessor.LONG_ARRAY);
  }

  @Override
  public Object setLongValue(Object storage, int index, long value) {
    Storage s = storage(storage, true);
    if (s.valueIndexes == null)
      s.valueIndexes = new BitSet2();
    s.valueIndexes.set(index, true);
    s.decoratedStorage = ((LongArrayAccessor) myAccessor).setLongValue(s.decoratedStorage, index, value);
    return s;
  }

  @Override
  public long getLongValue(Object storage, int index) {
    Storage s = storage(storage, false);
    if (s != null) {
      return ((LongArrayAccessor) myAccessor).getLongValue(s.decoratedStorage, index);
    }
    return 0;
  }
}
