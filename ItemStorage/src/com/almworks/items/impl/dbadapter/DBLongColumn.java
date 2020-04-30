package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.LongArrayAccessor;
import org.jetbrains.annotations.NotNull;

public class DBLongColumn extends DBColumn<Long> implements PhysicalColumnAdapter.Long {
  private final LongArrayAccessor myAccessor;

  public DBLongColumn(String name) {
    super(name);
    myAccessor = LongArrayAccessor.LONG_ARRAY;
  }

  public LongArrayAccessor getArrayAccessor() {
    return myAccessor;
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.INTEGER;
  }

  public java.lang.Long toUserValue(long databaseValue, boolean isnull) {
    return isnull ? 0L : databaseValue;
  }

  public Object storeNative(Object storage, int row, long value, boolean isNull) {
    return getArrayAccessor().setLongValue(storage, row, isNull ? 0 : value);
  }

  public java.lang.Long loadValue(Object storage, int row) {
    return getArrayAccessor().getLongValue(storage, row);
  }

  public java.lang.Long toDatabaseValue(java.lang.Long userValue) {
    if (userValue == null)
      return 0L;
    return userValue;
  }

  public long getLong(DBRowSet rowSet, int row) {
    return getArrayAccessor().getLongValue(rowSet.getColumnData(this), row);
  }

  public long getLong(DBRow row) {
    java.lang.Long value = row.getValue(this);
    return value != null ? value : 0;
  }

  public void setValue(DBRow row, long value) {
    java.lang.Long prev = row.getValue(this);
    if (prev == null || prev != value)
      row.setValue(this, value);
  }
}
