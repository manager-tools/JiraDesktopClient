package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Const;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ScalarAdapterByteArray extends ScalarValueAdapter<byte[]> {
  @Override
  public Class<byte[]> getAdaptedClass() {
    return byte[].class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return BLOB_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return false;
  }

  @Override
  public byte[] loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    return select.columnBlob(columnIndex);
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, byte[] userValue, TransactionContext context)
    throws SQLiteException
  {
    statement.bind(bindIndex, userValue);
  }

  @Override
  protected byte[] readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int size = CompactInt.readInt(in);
    if (size < 0) return null;
    if (size == 0) return Const.EMPTY_BYTES;
    byte[] value = new byte[size];
    in.readFully(value);
    return value;
  }

  @Override
  protected void writeValueToStream(DataOutput out, byte[] userValue, TransactionContext context) throws IOException {
    CompactInt.writeInt(out, userValue == null ? -1 : userValue.length);
    if (userValue != null && userValue.length > 0) {
      out.write(userValue);
    }
  }

  @Override
  public Object toSearchValue(byte[] userValue) {
    return null;
  }
}
