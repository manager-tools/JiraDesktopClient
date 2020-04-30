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

public class ScalarAdapterInteger extends ScalarValueAdapter<Integer> {
  @Override
  public Class<Integer> getAdaptedClass() {
    return Integer.class;
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
  public Integer loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context) throws SQLiteException {
    return select.columnInt(columnIndex);
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, Integer userValue, TransactionContext context) throws SQLiteException {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = 0;
    }
    int v = userValue;
    statement.bind(bindIndex, v);
  }

  @Override
  protected Integer readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int v = CompactInt.readInt(in);
    if (v == 0) {
      // suspect null
      int nullv = CompactInt.readInt(in);
      return nullv == 0 ? null : 0;
    }
    return v;
  }

  @Override
  protected void writeValueToStream(DataOutput out, Integer userValue, TransactionContext context) throws IOException {
    if (userValue == null) {
      CompactInt.writeInt(out, 0);
      CompactInt.writeInt(out, 0);
    } else {
      CompactInt.writeInt(out, userValue);
      if (userValue == 0) {
        CompactInt.writeInt(out, 1);
      }
    }
  }

  @Override
  public Object toSearchValue(Integer userValue) {
    return userValue;
  }
}

