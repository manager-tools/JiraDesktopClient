package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.BooleanArrayAccessor;
import org.jetbrains.annotations.NotNull;

public class DBBooleanColumn extends DBColumn<Boolean> implements PhysicalColumnAdapter.Long {
  public static final long FALSE = 0;
  public static final long TRUE = 1;
  private final BooleanArrayAccessor myAccessor;

  public DBBooleanColumn(String name) {
    super(name);
    myAccessor = BooleanArrayAccessor.BOOLEAN_ARRAY;
  }

  public BooleanArrayAccessor getArrayAccessor() {
    return myAccessor;
  }

  public boolean getValue(DBRowSet rowSet, int row) {
    return getArrayAccessor().getBoolValue(rowSet.getColumnData(this), row);
  }

  public Object storeNative(Object storage, int row, long value, boolean isNull) {
    return getArrayAccessor().setBoolValue(storage, row, !isNull && value != FALSE);
  }

  public Boolean toUserValue(long value, boolean isnull) {
    return !isnull && value != FALSE;
  }

  public Boolean loadValue(Object storage, int row) {
    return getArrayAccessor().getObjectValue(storage, row);
  }

  public java.lang.Long toDatabaseValue(Boolean userValue) {
    return userValue != null && userValue ? TRUE : FALSE;
  }

  @NotNull
  public DBColumnType getDatabaseClass() {
    return DBColumnType.INTEGER;
  }

  public boolean getValue(DBRow row) {
    Boolean value = row.getValue(this);
    return value == null ? getArrayAccessor().getZeroValue() : Boolean.TRUE.equals(value);
  }

  public void setValue(DBRow row, boolean value) {
    row.setValue(this, value);
  }
}
