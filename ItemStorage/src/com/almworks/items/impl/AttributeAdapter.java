package com.almworks.items.impl;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.impl.dbadapter.*;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.almworks.items.impl.dbadapter.DBColumn.ITEM;

public abstract class AttributeAdapter {
  public static final DBIntColumn POSITION = new DBIntColumn("position");

  protected final DBTable myDefinition;
  protected final ScalarValueAdapter myScalarAdapter;
  protected final DBColumn myScalarColumn;

  public AttributeAdapter(DBTable definition, ScalarValueAdapter scalarAdapter, DBColumn scalarColumn) {
    myDefinition = definition;
    myScalarAdapter = scalarAdapter;
    myScalarColumn = scalarColumn;
  }

  public DBTable getTable() {
    return myDefinition;
  }

  public Class<?> getScalarClass() {
    return myScalarAdapter.getAdaptedClass();
  }

  public ScalarValueAdapter getScalarAdapter() {
    return myScalarAdapter;
  }

  public DBColumn getScalarColumn() {
    return myScalarColumn;
  }

  public abstract Object readValue(long item, DBReaderImpl reader) throws SQLiteException;

  public abstract boolean writeValue(long item, Object value, DBWriterImpl writer) throws SQLiteException;

  public static AttributeAdapter create(DBAttribute<?> attribute, ScalarValueAdapter scalarAdapter) {
    DBTableBuilder builder = new DBTableBuilder(attribute.getId());

    DBColumn scalarColumn = scalarAdapter.getScalarColumn();
    DBAttribute.ScalarComposition composition = attribute.getComposition();

    switch (composition) {
    case SCALAR:
      builder.itempk().column(scalarColumn, true);
      break;
    case SET:
      builder.column(ITEM, true).column(scalarColumn, true).pk(ITEM, scalarColumn);
      break;
    case LIST:
      builder.column(ITEM, true).column(POSITION, true).pk(ITEM, POSITION).column(scalarColumn, true);
      break;
    }

    if (scalarAdapter.isIndexable()) {
      builder.index(scalarColumn);
    }

    DBTable definition = builder.create();
    switch (composition) {
    case SCALAR:
      return new ScalarAdapter(definition, scalarAdapter, scalarColumn);
    case SET:
      return new SetAdapter(definition, scalarAdapter, scalarColumn);
    case LIST:
      return new ListAdapter(definition, scalarAdapter, scalarColumn);
    }
    throw new Error();
  }

  protected void selectSql(SQLParts parts, String table) {
    parts.append("SELECT ")
      .append(myScalarColumn.getName())
      .append(" FROM ")
      .append(table)
      .append(" WHERE ")
      .append(DBColumn.ITEM.getName())
      .append(" = ?");
  }

  protected static void deleteSql(SQLParts parts, String table) {
    parts.append("DELETE FROM ").append(table).append(" WHERE ").append(DBColumn.ITEM.getName()).append(" = ?");
  }


  protected void deleteValues(long item, TransactionContext context) throws SQLiteException {
    SQLiteStatement update = SQLiteStatement.DISPOSED;
    try {
      String table = context.getTableName(myDefinition, false);
      if (table != null) {
        SQLParts parts = context.sql();
        deleteSql(parts, table);
        update = context.prepare(parts);
        update.bind(1, item);
        context.addCancellable(update);
        update.step();
      }
    } finally {
      context.removeCancellable(update);
      update.dispose();
    }
  }

  public LongList loadItemIdValues(long item, TransactionContext context) throws SQLiteException {
    if (!(myScalarAdapter.getScalarColumn() instanceof DBLongColumn)) {
      Log.error("cannot load item ids from non-long attribute");
      return LongList.EMPTY;
    }
    SQLiteStatement select = SQLiteStatement.DISPOSED;
    try {
      String table = context.getTableName(myDefinition, false);
      if (table == null)
        return LongList.EMPTY;
      SQLParts parts = context.sql();
      selectSql(parts, table);
      select = context.prepare(parts);
      select.bind(1, item);
      context.addCancellable(select);
      LongArray r = new LongArray();
      SQLUtil.loadLongs(select, context, r);
      return r;
    } finally {
      context.removeCancellable(select);
      select.dispose();
    }
  }

  public abstract Object readValueFromStream(DataInput in, TransactionContext context) throws IOException;

  public abstract void writeValueToStream(DataOutput out, Object userValue, TransactionContext context)
    throws IOException;

  public abstract Object arrayGet(Object storage, int index);

  public abstract Object arrayLoad(LongList itemsSorted, @Nullable SQLiteLongArray itemsArray,
    TransactionContext context) throws SQLiteException;

  public abstract NullableArrayStorageAccessor getArrayAccessor();
}
