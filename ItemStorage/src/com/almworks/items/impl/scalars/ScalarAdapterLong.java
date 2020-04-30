package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import com.almworks.util.collections.arrays.NullableLongArrayAccessor;
import org.almworks.util.Log;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

public class ScalarAdapterLong extends ScalarValueAdapter<Long> {
  private static final NullableLongArrayAccessor ACCESSOR = NullableLongArrayAccessor.LONG_ARRAY_NULLABLE;

  @Override
  public Class<Long> getAdaptedClass() {
    return Long.class;
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
  public Long loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    return select.columnLong(columnIndex);
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, Long userValue, TransactionContext context)
    throws SQLiteException
  {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = 0L;
    }
    long v = userValue;
    statement.bind(bindIndex, v);
  }

  @Override
  public void loadUserValuesInto(SQLiteStatement select, int columnIndex, final Collection target,
    TransactionContext context) throws SQLiteException
  {
    if (columnIndex != 0) {
      super.loadUserValuesInto(select, columnIndex, target, context);
      return;
    }
    // todo actually, it's not items - it's long values. this may cause confusion where it comes to duplicates
    SQLUtil.loadItems(select, context, new ItemVisitor.ForEachItem() {
      @Override
      protected boolean visitItem(long item) throws SQLiteException {
        target.add(item);
        return true;
      }
    });
  }

  @Override
  protected Long readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    long v = CompactInt.readLong(in);
    if (v == 0) {
      // suspect null
      int nullv = CompactInt.readInt(in);
      return nullv == 0 ? null : 0L;
    }
    return v;
  }

  @Override
  protected void writeValueToStream(DataOutput out, Long userValue, TransactionContext context) throws IOException {
    if (userValue == null) {
      CompactInt.writeLong(out, 0);
      CompactInt.writeInt(out, 0);
    } else {
      CompactInt.writeLong(out, userValue);
      if (userValue == 0) {
        CompactInt.writeInt(out, 1);
      }
    }
  }

  @Override
  public Object toSearchValue(Long userValue) {
    return userValue;
  }

  @Override
  public NullableArrayStorageAccessor getNullableAccessor() {
    return ACCESSOR;
  }

  @Override
  public Object loadNullableUserValueIntoArrayStorage(Object storage, int index, SQLiteStatement select,
    int columnIndex, TransactionContext context) throws SQLiteException
  {
    if (select.columnNull(columnIndex)) {
      storage = ACCESSOR.setNull(storage, index);
    } else {
      storage = ACCESSOR.setLongValue(storage, index, select.columnLong(columnIndex));
    }
    return storage;
  }
}

