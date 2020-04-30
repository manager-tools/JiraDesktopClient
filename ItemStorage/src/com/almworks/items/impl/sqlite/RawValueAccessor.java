package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.PhysicalColumnAdapter;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public abstract class RawValueAccessor {
  private static final NativeIntAccessor NATIVE_INT_ACCESSOR = new NativeIntAccessor();
  private static final DefaultAccessor DEFAULT_ACCESSOR = new DefaultAccessor();

  public abstract Object getValue(DBColumn column, SQLiteStatement select, int index) throws SQLiteException;

  public abstract Object addValue(Object storage, int row, DBColumn column, SQLiteStatement select, int columnIndex)
    throws SQLiteException;

  public abstract Object setNull(Object storage, int row, DBColumn column);

  public static RawValueAccessor getAccessor(DBColumn column) throws SQLiteException {
    if (column instanceof PhysicalColumnAdapter.Long)
      return NATIVE_INT_ACCESSOR;
    if (column instanceof PhysicalColumnAdapter.Any)
      return DEFAULT_ACCESSOR;
    assert false : column;
    throw new SQLiteException(SQLiteConstants.WRAPPER_USER_ERROR, "Wrong column class: " + column);
  }

  private static class DefaultAccessor extends RawValueAccessor {
    public final Object getValue(DBColumn column, SQLiteStatement select, int index) throws SQLiteException {
      return ((PhysicalColumnAdapter.Any) column).toUserValue(load(select, index));
    }

    public final Object addValue(Object storage, int row, DBColumn column, SQLiteStatement select, int columnIndex)
      throws SQLiteException
    {
      return ((PhysicalColumnAdapter.Any) column).storeNative(storage, row, load(select, columnIndex));
    }

    public Object setNull(Object storage, int row, DBColumn column) {
      return ((PhysicalColumnAdapter.Any) column).storeNative(storage, row, null);
    }

    protected Object load(SQLiteStatement select, int index) throws SQLiteException {
      return select.columnValue(index);
    }
  }


  protected static class NativeIntAccessor extends RawValueAccessor {
    public Object getValue(DBColumn column, SQLiteStatement select, int index) throws SQLiteException {
      boolean isnull = select.columnNull(index);
      long value = isnull ? 0 : select.columnLong(index);
      return ((PhysicalColumnAdapter.Long) column).toUserValue(value, isnull);
    }

    public Object addValue(Object storage, int row, DBColumn column, SQLiteStatement select, int columnIndex)
      throws SQLiteException
    {
      boolean isnull = select.columnNull(columnIndex);
      long value = isnull ? 0 : select.columnLong(columnIndex);
      return ((PhysicalColumnAdapter.Long) column).storeNative(storage, row, value, isnull);
    }

    public Object setNull(Object storage, int row, DBColumn column) {
      return ((PhysicalColumnAdapter.Long) column).storeNative(storage, row, 0, true);
    }
  }
}
