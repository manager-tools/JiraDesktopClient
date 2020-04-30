package com.almworks.items.impl;

import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Collections15;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListAdapter extends CollectionAdapter {
  public ListAdapter(DBTable definition, ScalarValueAdapter scalarClass, DBColumn scalarColumn) {
    super(definition, scalarClass, scalarColumn);
  }

  @Override
  protected Collection emptyCollection() {
    return Collections.emptyList();
  }

  @Override
  protected Collection createCollection() {
    return Collections15.arrayList();
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
      parts.append(" ORDER BY ").append(POSITION.getName());
      select = context.prepare(parts);
      select.bind(1, item);
      context.addCancellable(select);
      List result = new ArrayList();
      myScalarAdapter.loadUserValuesInto(select, 0, result, context);
      return result.isEmpty() ? null : result;
    } finally {
      context.removeCancellable(select);
      select.dispose();
    }
  }

  @Override
  public boolean writeValue(long item, Object value, DBWriterImpl writer) throws SQLiteException {
    Collection col = (Collection) value;
    deleteValues(item, writer.getContext());
    if (col == null || col.isEmpty()) {
      return true;
    }
    insertCollection(item, col, writer, true);
    return true;
  }

  @Override
  public Object readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int length = CompactInt.readInt(in);
    if (length < 0)
      return null;
    ArrayList r = new ArrayList();
    for (int i = 0; i < length; i++) {
      r.add(myScalarAdapter.readValueFromStream(in, context));
    }
    return r;
  }

  @Override
  protected void orderBy(SQLParts parts) {
    parts.append(" ORDER BY ").append(DBColumn.ITEM.getName()).append(", ").append(POSITION.getName());
  }
}
