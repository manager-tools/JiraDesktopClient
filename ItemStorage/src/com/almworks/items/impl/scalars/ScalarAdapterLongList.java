package com.almworks.items.impl.scalars;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactInt;

import java.io.*;

public class ScalarAdapterLongList extends ScalarValueAdapter<LongList> {
  @Override
  public Class<LongList> getAdaptedClass() {
    return LongList.class;
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
  public LongList loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    InputStream is = select.columnStream(columnIndex);
    if (is == null)
      return null;
    try {
      DataInput in = new DataInputStream(is);
      return readValueFromStream(in, context);
    } catch (IOException e) {
      Log.warn("cannot read value", e);
      return null;
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Override
  protected LongList readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int size = CompactInt.readInt(in);
    if (size < 0) return null;
    if (size == 0) return LongList.EMPTY;
    LongArray r = new LongArray(size);
    for (int i = 0; i < size; i++) {
      r.add(CompactInt.readLong(in));
    }
    return r;
  }

  @Override
  protected void writeValueToStream(DataOutput out, LongList userValue, TransactionContext context)
    throws IOException
  {
    if (userValue == null) {
      CompactInt.writeInt(out, -1);
      return;
    }
    CompactInt.writeInt(out, userValue.size());
    for (LongIterator ii = userValue.iterator(); ii.hasNext(); ) {
      CompactInt.writeLong(out, ii.nextValue());
    }
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, LongList userValue,
    TransactionContext context) throws SQLiteException
  {
    OutputStream os = statement.bindStream(bindIndex);
    try {
      DataOutputStream out = new DataOutputStream(os);
      writeValueToStream(out, userValue, context);
      out.close();
    } catch (IOException e) {
      Log.warn("cannot write value", e);
      statement.bindZeroBlob(bindIndex, 0);
    } finally {
      try {
        os.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Override
  public Object toSearchValue(LongList userValue) {
    return null;
  }
}

