package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.IntArrayAccessor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class DBIntColumn extends DBColumn<Integer> implements PhysicalColumnAdapter.Long, Serializable {
  private final IntArrayAccessor myAccessor;

  public DBIntColumn(String name) {
    super(name);
    myAccessor = IntArrayAccessor.INT_ARRAY;
  }

  public IntArrayAccessor getArrayAccessor() {
    return myAccessor;
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.INTEGER;
  }

  public Object storeNative(Object storage, int row, long value, boolean isNull) {
    int intValue = intValue(value, isNull);
    return getArrayAccessor().setIntValue(storage, row, intValue);
  }

  public Integer loadValue(Object storage, int row) {
    IntArrayAccessor accessor = getArrayAccessor();
    return accessor.getIntValue(storage, row);
  }

  public Object toUserValue(long value, boolean isnull) {
    return intValue(value, isnull);
  }

  private int intValue(long value, boolean isnull) {
    if (isnull)
      return 0;
    if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
      return 0;
    return (int) value;
  }

  public Integer toDatabaseValue(Integer userValue) {
    if (userValue == null)
      userValue = 0;
    return userValue;
  }

  public int getInt(DBRowSet result, int row) {
    return getArrayAccessor().getIntValue(result.getColumnData(this), row);
  }

  public int getInt(DBRow row) {
    Integer value = row.getValue(this);
    return value != null ? value : 0;
  }

  public void setValue(DBRow row, int value) {
    if (row instanceof DBRowSingleColumnLong) {
      ((DBRowSingleColumnLong) row).setValue(value);
      return;
    }
    Integer prev = row.getValue(this);
    if (prev == null || prev != value) {
      row.setValue(this, value);
    }
  }
}
