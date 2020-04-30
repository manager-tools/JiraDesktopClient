package com.almworks.items.impl;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import com.almworks.util.collections.arrays.ObjectArrayAccessor;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;
import util.external.CompactInt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

public abstract class CollectionAdapter extends AttributeAdapter {
  CollectionAdapter(DBTable definition, ScalarValueAdapter scalarAdapter, DBColumn scalarColumn) {
    super(definition, scalarAdapter, scalarColumn);
  }

  @Override
  public void writeValueToStream(DataOutput out, Object userValue, TransactionContext context) throws IOException {
    if (userValue == null) {
      CompactInt.writeInt(out, -1);
      return;
    }
    Collection col = (Collection) userValue;
    CompactInt.writeInt(out, col.size());
    for (Object v : col) {
      myScalarAdapter.writeValueToStream(out, v, context);
    }
  }

  @Override
  public Object arrayGet(Object storage, int index) {
    Object scalarArray = ObjectArrayAccessor.INSTANCE.getObjectValue(storage, index);
    if (scalarArray == null)
      return emptyCollection();
    Collection target = createCollection();
    NullableArrayStorageAccessor accessor = myScalarAdapter.getNullableAccessor();
    int i = 0;
    while (true) {
      Object value = accessor.getObjectValue(scalarArray, i);
      if (value == null)
        break;
      target.add(value);
    }
    return target;
  }

  protected abstract Collection emptyCollection();

  protected abstract Collection createCollection();


  protected void insertCollection(long item, Collection col, DBWriterImpl writer, boolean usePosition)
    throws SQLiteException
  {
    TransactionContext context = writer.getContext();
    SQLiteStatement update = SQLiteStatement.DISPOSED;
    try {
      String table = context.getTableName(myDefinition, true);
      SQLParts parts =
        context.sql().append("INSERT INTO ").append(table).append(" (").append(DBColumn.ITEM.getName()).append(", ");
      if (usePosition) {
        parts.append(POSITION.getName()).append(", ");
      }
      parts.append(myScalarColumn.getName());
      if (usePosition) {
        parts.append(") VALUES (?, ?, ?)");
      } else {
        parts.append(") VALUES (?, ?)");
      }
      update = context.prepare(parts);
      context.addCancellable(update);
      update.bind(1, item);
      int index = 0;
      for (Object userValue : col) {
        if (!myScalarAdapter.isMaterialValue(userValue)) {
          continue;
        }
        if (usePosition) {
          update.bind(2, index);
          myScalarAdapter.bindParameter(update, 3, userValue, context);
        } else {
          myScalarAdapter.bindParameter(update, 2, userValue, context);
        }
        update.step();
        update.reset(false);
        index++;
      }
    } finally {
      context.removeCancellable(update);
      update.dispose();
    }
  }

  @Override
  public Object arrayLoad(LongList itemsSorted, @Nullable SQLiteLongArray itemsArray, TransactionContext context)
    throws SQLiteException
  {
    String table = context.getTableName(myDefinition, false);
    if (table == null) {
      return null;
    }
    boolean disposeArray = itemsArray == null;
    if (disposeArray) {
      itemsArray = context.useArray(itemsSorted);
    }
    try {
      SQLParts parts = context.sql()
        .append("SELECT ")
        .append(DBColumn.ITEM.getName())
        .append(", ")
        .append(myScalarColumn.getName())
        .append(" FROM ")
        .append(table)
        .append(" WHERE ")
        .append(DBColumn.ITEM.getName())
        .append(" IN ")
        .append(itemsArray.getName());
      orderBy(parts);
      SQLiteStatement select = context.prepare(parts);
      try {
        Object result = null;
        LongIterator ii = itemsSorted.iterator();
        long nextItem = ii.hasNext() ? -1 : ii.nextValue();
        int itemIndex = 0;
        select.step();
        while (select.hasRow()) {
          long item = select.columnLong(0);
          while (nextItem < item && nextItem >= 0) {
            result = ObjectArrayAccessor.INSTANCE.setNull(result, itemIndex);
            nextItem = ii.hasNext() ? -1 : ii.nextValue();
            itemIndex++;
          }
          if (nextItem != item) {
            Log.error("inconsistent result " + nextItem + " " + item);
          }
          Object value = null;
          int valueIndex = 0;
          do {
            value = myScalarAdapter.loadNullableUserValueIntoArrayStorage(value, valueIndex++, select, 1, context);
            select.step();
          } while (select.hasRow() && select.columnLong(0) == item);
          result = ObjectArrayAccessor.INSTANCE.setObjectValue(result, itemIndex, value);
          itemIndex++;
        }
        while (nextItem >= 0) {
          result = ObjectArrayAccessor.INSTANCE.setNull(result, itemIndex);
          nextItem = ii.hasNext() ? -1 : ii.nextValue();
          itemIndex++;
        }
        if (itemIndex != itemsSorted.size()) {
          Log.error(this + ": inconsistent result [" + itemIndex + "][" + itemsSorted + "]");
        }
        return result;
      } finally {
        select.dispose();
      }
    } finally {
      if (disposeArray) {
        itemsArray.dispose();
      }
    }
  }

  @Override
  public NullableArrayStorageAccessor getArrayAccessor() {
    return ObjectArrayAccessor.INSTANCE;
  }

  protected abstract void orderBy(SQLParts parts);
}
