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
import java.util.Date;

public class ScalarAdapterDate extends ScalarValueAdapter<Date> {
  @Override
  public Class<Date> getAdaptedClass() {
    return Date.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return LONG_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return true;
  }

  @Override
  public Date loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    return toUserValue(select.columnLong(columnIndex));
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, Date userValue, TransactionContext context)
    throws SQLiteException
  {
    long dbv = toDatabaseValue(userValue);
    if (userValue == null) {
      Log.error("cannot bind null");
    }
    statement.bind(bindIndex, dbv);
  }

  @Override
  protected Date readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    return toUserValue(CompactInt.readLong(in));
  }

  @Override
  protected void writeValueToStream(DataOutput out, Date userValue, TransactionContext context)
    throws IOException
  {
    CompactInt.writeLong(out, toDatabaseValue(userValue));
  }

  @Override
  public Object toSearchValue(Date userValue) {
    return toDatabaseValue(userValue);
  }

  private long toDatabaseValue(Date userValue) {
    return userValue == null ? 0 : userValue.getTime();
  }

  private Date toUserValue(long dbValue) {
    try {
      return new Date(dbValue);
    } catch (RuntimeException e) {
      Log.warn(e);
      return null;
    }
  }
}

