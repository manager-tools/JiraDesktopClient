package com.almworks.util.collections.arrays;

import util.external.BitSet2;

/**
 * Decorates not-nullable 
 */
public class NullableArrayStorageDecorator implements NullableArrayStorageAccessor {
  protected final ArrayStorageAccessor myAccessor;

  public NullableArrayStorageDecorator(ArrayStorageAccessor accessor) {
    myAccessor = accessor;
  }

  @Override
  public boolean isNull(Object storage, int index) {
    Storage s = storage(storage, false);
    return s == null || s.isNull(index);
  }
  
  @Override
  public Object setNull(Object storage, int index) {
    Storage s = storage(storage, false);
    if (s != null && s.valueIndexes != null) {
      s.valueIndexes.set(index, false);
    }
    return s;
  }

  @Override
  public Object copyValue(Object src, int srcIndex, Object dst, int dstIndex) {
    return setObjectValue(dst, dstIndex, getObjectValue(src, srcIndex));
  }

  @Override
  public Object getObjectValue(Object storage, int index) {
    Storage s = storage(storage, false);
    if (s == null) return null;
    if (s.isNull(index)) return null;
    return myAccessor.getObjectValue(s.decoratedStorage, index);
  }

  @Override
  public Object setObjectValue(Object storage, int index, Object value) {
    if (value == null) {
      return setNull(storage, index);
    }
    Storage s = storage(storage, true);
    if (s.valueIndexes == null) s.valueIndexes = new BitSet2();
    s.valueIndexes.set(index, true);
    s.decoratedStorage = myAccessor.setObjectValue(s.decoratedStorage, index, value);
    return s;
  }

  @Override
  public Object shiftRight(Object storage, int offset, int count) {
    Storage s = storage(storage, false);
    if (s != null) {
      if (s.valueIndexes != null) {
        s.valueIndexes.shiftRight(offset, count);
      }
      s.decoratedStorage = myAccessor.shiftRight(s.decoratedStorage, offset, count);
    }
    return s;
  }

  @Override
  public Object shiftLeft(Object storage, int offset, int count) {
    Storage s = storage(storage, false);
    if (s != null) {
      if (s.valueIndexes != null) {
        s.valueIndexes.shiftLeft(offset, count);
      }
      s.decoratedStorage = myAccessor.shiftLeft(s.decoratedStorage, offset, count);
    }
    return s;
  }

  protected Storage storage(Object storage, boolean create) {
    if (storage instanceof Storage) return (Storage) storage;
    return create ? new Storage() : null;
  }

  protected static class Storage {
    public Object decoratedStorage;
    public BitSet2 valueIndexes;

    boolean isNull(int index) {
      return valueIndexes == null || !valueIndexes.get(index);
    }
  }
}
