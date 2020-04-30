package com.almworks.items.impl;

import com.almworks.items.impl.dbadapter.*;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import com.almworks.util.collections.arrays.ObjectArrayAccessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

/**
 * Manages conversions of scalar values of type T to and from database values.
 *
 * @param <T>
 */
public abstract class ScalarValueAdapter<T> {
  protected static final DBStringColumn STRING_VALUE = new DBStringColumn("value");
  protected static final DBIntColumn INT_VALUE = new DBIntColumn("value");
  protected static final DBLongColumn LONG_VALUE = new DBLongColumn("value");
  protected static final DBBooleanColumn BOOL_VALUE = new DBBooleanColumn("value");
  protected static final DBByteArrayColumn BLOB_VALUE = new DBByteArrayColumn("value");

  public abstract Class<T> getAdaptedClass();

  public abstract DBColumn getScalarColumn();

  public abstract boolean isIndexable();

  public abstract T loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException;

  public abstract void bindParameter(SQLiteStatement statement, int bindIndex, T userValue, TransactionContext context)
    throws SQLiteException;

  protected abstract T readValueFromStream(DataInput in, TransactionContext context) throws IOException;

  protected abstract void writeValueToStream(DataOutput out, T userValue, TransactionContext context)
    throws IOException;

  public abstract Object toSearchValue(T userValue);

  public boolean isMaterialValue(T value) {
    return value != null;
  }

  public void loadUserValuesInto(SQLiteStatement select, int columnIndex, Collection target, TransactionContext context)
    throws SQLiteException
  {
    while (select.step()) {
      target.add(loadUserValue(select, columnIndex, context));
    }
  }

  public NullableArrayStorageAccessor getNullableAccessor() {
    return ObjectArrayAccessor.INSTANCE;
  }

  public Object loadNullableUserValueIntoArrayStorage(Object storage, int index, SQLiteStatement select,
    int columnIndex, TransactionContext context) throws SQLiteException
  {
    NullableArrayStorageAccessor accessor = getNullableAccessor();
    if (select.columnNull(columnIndex)) {
      storage = accessor.setNull(storage, index);
    } else {
      storage = accessor.setObjectValue(storage, index, loadUserValue(select, columnIndex, context));
    }
    return storage;
  }
}
