package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import org.almworks.util.Log;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ScalarAdapter extends AttributeAdapter {
  public ScalarAdapter(DBTable definition, ScalarValueAdapter scalarClass, DBColumn scalarColumn) {
    super(definition, scalarClass, scalarColumn);
  }

  @Override
  public Object readValue(long item, DBReaderImpl reader) throws SQLiteException {
    TransactionContext context = reader.getContext();
    SQLiteStatement select = SQLiteStatement.DISPOSED;
    try {
      String table = context.getTableName(myDefinition, false);
      if (table == null)
        return null;
      SQLParts parts = context.sql();
      selectSql(parts, table);
      select = context.prepare(parts);
      select.bind(1, item);
      context.addCancellable(select);
      boolean hasRow = select.step();
      if (hasRow) {
        return myScalarAdapter.loadUserValue(select, 0, context);
      } else {
        return null;
      }
    } finally {
      context.removeCancellable(select);
      select.dispose();
    }
  }

  @Override
  public boolean writeValue(long item, Object value, DBWriterImpl writer) throws SQLiteException {
    TransactionContext context = writer.getContext();
    if (!myScalarAdapter.isMaterialValue(value)) {
      deleteValues(item, context);
      return true;
    }

    SQLiteStatement update = SQLiteStatement.DISPOSED;
    try {
      SQLParts parts = context.sql();
      String table = context.getTableName(myDefinition, true);
      parts.append("INSERT OR REPLACE INTO ")
        .append(table)
        .append(" (")
        .append(DBColumn.ITEM.getName())
        .append(", ")
        .append(myScalarColumn.getName())
        .append(") VALUES (?, ?)");
      update = context.prepare(parts);
      update.bind(1, item);
      assert value == null || myScalarAdapter.getAdaptedClass().isInstance(value) : myDefinition + "." + myScalarColumn + " " + value;
      myScalarAdapter.bindParameter(update, 2, value, context);
      context.addCancellable(update);
      update.step();
    } finally {
      context.removeCancellable(update);
      update.dispose();
    }
    return true;
  }

  @Override
  public Object readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    return myScalarAdapter.readValueFromStream(in, context);
  }

  @Override
  public void writeValueToStream(DataOutput out, Object userValue, TransactionContext context) throws IOException {
    myScalarAdapter.writeValueToStream(out, userValue, context);
  }

  @Override
  public Object arrayGet(Object storage, int index) {
    return myScalarAdapter.getNullableAccessor().getObjectValue(storage, index);
  }

  @Override
  public Object arrayLoad(LongList itemsSorted, SQLiteLongArray itemsArray, TransactionContext context)
    throws SQLiteException
  {
    String table = context.getTableName(myDefinition, false);
    if (table == null) {
      return null;
    }
    String item = DBColumn.ITEM.getName();
    SQLiteStatement st = context.prepare(context.sql()
      .append("SELECT t1.value, t2.")
      .append(myScalarColumn.getName())
      .append(" FROM ")
      .append(itemsArray.getName())
      .append(" t1 LEFT OUTER JOIN ")
      .append(table)
      .append(" t2 ON t1.value = t2.")
      .append(item).append(" ORDER BY 1"));
    try {
      Object result = null;
      int index = 0;
      while (st.step()) {
        if (index >= itemsSorted.size() || st.columnLong(0) != itemsSorted.get(index)) {
          Log.error(this + ": inconsistent result [" + index + "]");
        }
        result = myScalarAdapter.loadNullableUserValueIntoArrayStorage(result, index, st, 1, context);
        index++;
      }
      if (index != itemsSorted.size()) {
        Log.error(this + ": inconsistent result [" + index + "][" + itemsSorted + "]");
      }
      return result;
    } finally {
      st.dispose();
    }
  }

  @Override
  public NullableArrayStorageAccessor getArrayAccessor() {
    return myScalarAdapter.getNullableAccessor();
  }
}
