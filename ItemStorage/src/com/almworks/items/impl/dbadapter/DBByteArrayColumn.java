package com.almworks.items.impl.dbadapter;

import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class DBByteArrayColumn extends BaseObjectColumn<byte[]> {
  public DBByteArrayColumn(String name) {
    super(name);
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.BLOB;
  }

  public byte[] toUserValue(Object value) {
    assert value == null || value instanceof byte[] : value;
    //noinspection ConstantConditions
    return (byte[]) value;
  }

  public boolean areEqual(byte[] value1, byte[] value2) {
    return ArrayUtil.equals(value1, value2);
  }
}
