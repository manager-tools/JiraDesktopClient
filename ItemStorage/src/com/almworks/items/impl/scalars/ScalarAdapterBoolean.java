package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ScalarAdapterBoolean extends ScalarValueAdapter<Boolean> {
  @Override
  public Class<Boolean> getAdaptedClass() {
    return Boolean.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return INT_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return true;
  }

  @Override
  public Boolean loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    int r = select.columnInt(columnIndex);
    if (r != 0 && r != 1) {
      Log.warn("boolean value is not [0..1]: " + r);
    }
    return r != 0;
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, Boolean userValue, TransactionContext context)
    throws SQLiteException
  {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = false;
    }
    int v = userValue ? 1 : 0;
    statement.bind(bindIndex, v);
  }

  @Override
  protected Boolean readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int v = CompactInt.readInt(in);
    return v < 0 ? null : v > 0;
  }

  @Override
  protected void writeValueToStream(DataOutput out, Boolean userValue, TransactionContext context) throws IOException {
    int v = userValue == null ? -1 : (userValue ? 1 : 0);
    CompactInt.writeInt(out, v);
  }

  @Override
  public Object toSearchValue(Boolean userValue) {
    return userValue != null && userValue ? 1 : 0;
  }
}

