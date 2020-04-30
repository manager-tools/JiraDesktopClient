package com.almworks.util.collections.arrays;

public class BooleanArrayAccessor implements ArrayStorageAccessor {
  public static final BooleanArrayAccessor BOOLEAN_ARRAY = new BooleanArrayAccessor();
  private static final IntArrayAccessor INT_ACCESSOR = IntArrayAccessor.INT_ARRAY;

  private static final int BIT_INDEX_BITS = 5;
  private static final int ELEMENT_MASK = 0xFFFFFFFF << BIT_INDEX_BITS;
  private static final int BIT_INDEX_MASK = ~ELEMENT_MASK;
  private static final int HIGH_BIT_MASK = 1 << 31;

  public Object copyValue(Object src, int srcIndex, Object dst, int dstIndex) {
    boolean srcValue = getBoolValue(src, srcIndex);
    return setBoolValue(dst, dstIndex, srcValue);
  }

  public Boolean getObjectValue(Object storage, int index) {
    return getBoolValue(storage, index);
  }


  public Object removeAt(Object storage, int index) {
    int intIndex = index >> BIT_INDEX_BITS;
    int arrayLength = INT_ACCESSOR.getArrayLength(storage);
    if (arrayLength <= intIndex)
      return storage;
    int prevBit = 0;
    for (int i = arrayLength - 1; i > intIndex; i--) {
      int intValue = INT_ACCESSOR.getIntValue(storage, i);
      int nextBit = (intValue & 1) != 0 ? (HIGH_BIT_MASK) : 0;
      intValue = ((intValue >> 1) & ~HIGH_BIT_MASK) | prevBit;
      storage = INT_ACCESSOR.setIntValue(storage, i, intValue);
      prevBit = nextBit;
    }
    int bitIndex = index & BIT_INDEX_MASK;
    int val = INT_ACCESSOR.getIntValue(storage, intIndex);
    if (bitIndex != 31) {
      int hiMask = HIGH_BIT_MASK >> (31 - bitIndex);
      int lowMask = ~hiMask;
      val = ((val >> 1) & hiMask) | (val & lowMask);
    }
    val = (val & ~HIGH_BIT_MASK) | prevBit;
    return INT_ACCESSOR.setIntValue(storage, intIndex, val);
  }

  public Object setNull(Object storage, int index) {
    return setBoolValue(storage, index, false);
  }

  public boolean getZeroValue() {
    return false;
  }

  @Override
  public Object shiftRight(Object storage, int offset, int count) {
    throw new Error(); // todo remove boolean accessor?
  }

  @Override
  public Object shiftLeft(Object storage, int offset, int count) {
    throw new Error(); // todo remove boolean accessor?
  }

  @Override
  public Object setObjectValue(Object storage, int index, Object value) {
    return setBoolValue(storage, index, value instanceof Boolean && (Boolean) value);
  }

  public Object setBoolValue(Object storage, int index, boolean value) {
    int intIndex = index >> BIT_INDEX_BITS;
    int intValue = INT_ACCESSOR.getIntValue(storage, intIndex);
    int bitIndex = index & BIT_INDEX_MASK;
    int dstValueMask = 1 << bitIndex;
    intValue = (intValue & ~dstValueMask) | ((value ? 1 : 0) << bitIndex);
    return INT_ACCESSOR.setIntValue(storage, intIndex, intValue);
  }

  public boolean getBoolValue(Object storage, int index) {
    int intIndex = index >> BIT_INDEX_BITS;
    int intValue = INT_ACCESSOR.getIntValue(storage, intIndex);
    return (intValue & (1 << (index & BIT_INDEX_MASK))) != 0;
  }
}
