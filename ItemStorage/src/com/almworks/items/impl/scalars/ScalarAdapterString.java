package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ScalarAdapterString extends ScalarValueAdapter<String> {
  @Override
  public Class<String> getAdaptedClass() {
    return String.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return STRING_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return false;
  }

  @Override
  public String loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    return select.columnString(columnIndex);
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, String userValue, TransactionContext context)
    throws SQLiteException
  {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = "";
    }
    statement.bind(bindIndex, userValue);
  }

  @Override
  protected String readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    return CompactChar.readString(in);
  }

  @Override
  protected void writeValueToStream(DataOutput out, String userValue, TransactionContext context) throws IOException {
    CompactChar.writeString(out, userValue);
  }

  @Override
  public Object toSearchValue(String userValue) {
    return userValue;
  }
}

