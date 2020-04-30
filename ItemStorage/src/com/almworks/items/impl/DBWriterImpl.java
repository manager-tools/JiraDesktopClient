package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBException;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBWriter;
import com.almworks.items.impl.sqlite.Schema;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.util.AttributeMap;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

public class DBWriterImpl extends DBReaderImpl implements DBWriter {
  private final WriteHandle<?> myWriteHandle;

  public DBWriterImpl(TransactionContext context, WriteHandle<?> writeHandle) {
    super(context);
    if (!context.isWriteAllowed())
      throw new IllegalArgumentException(context + " does not allow writing");
    myWriteHandle = writeHandle;
  }

  public <T> void setValue(long item, DBAttribute<T> attribute, @Nullable T value) {
    if (item <= 0) throw new IllegalArgumentException("Item " + item + " attribute " + attribute);
    setValue0(item, attribute, value);
    TransactionContext context = getContext();
    notifyHooks(context, item, attribute, value, context.getTransactionCache());
    notifyHooks(context, item, attribute, value, context.getSessionContext().getSessionCache());
    materialize(attribute);
  }

  private <T> void notifyHooks(TransactionContext context, long item, DBAttribute<T> attribute, T value, Map cache) {
    for (Iterator ii = cache.values().iterator(); ii.hasNext();) {
      Object object = ii.next();
      if (object instanceof WriteHook) {
        try {
          ((WriteHook) object).onSetValue(context, item, attribute, value);
        } catch (Exception e) {
          Log.warn(object + " could not process hook (" + item + ", " + attribute + ", " + value + ")", e);
          ii.remove();
        }
      }
    }
  }

  private <T> AttributeAdapter setValue0(long item, DBAttribute<T> attribute, T value) {
    try {
      AttributeAdapter adapter = getAttributeAdapter(attribute);
      LongList oldReferences = LongList.EMPTY;
      if (attribute.isPropagatingChange()) {
        oldReferences = adapter.loadItemIdValues(item, myContext);
      }
      boolean changed = adapter.writeValue(item, value, this);
      if (changed) {
        itemChanged(item);
        if (!oldReferences.isEmpty()) {
          propagateChangeToOldReferences(oldReferences);
        }
      }
      return adapter;
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  private void propagateChangeToOldReferences(LongList list) {
    for (int i = 0; i < list.size(); i++) {
      itemChanged(list.get(i));
    }
  }

  public long nextItem() {
    long item;
    SQLiteStatement st = SQLiteStatement.DISPOSED;
    try {
      st = myContext.prepare(myContext.sql()
        .append("INSERT INTO ")
        .append(Schema.ITEMS)
        .append(" (")
        .append(Schema.ITEMS_LAST_ICN.getName()).append(") VALUES (?)"));
      st.bind(1, myContext.getIcn());
      st.step();
      item = myContext.getConnection().getLastInsertId();
      itemChanged(item);
      return item;
    } catch (SQLiteException e) {
      throw new DBException(e);
    } finally {
      st.dispose();
    }
  }

  public void itemChanged(long item) {
    if (item <= 0)
      return;
    myContext.itemChanged(item);
  }

  @Override
  public long getItemIcn(long item) {
    return getItemIcnNoCache(item);
  }

  @Override
  public void finallyDo(ThreadGate gate, final Procedure<Boolean> procedure) {
    myWriteHandle.finallyDo(gate, new Procedure<Object>() {
      @Override
      public void invoke(Object arg) {
        procedure.invoke(myWriteHandle.isSuccessful());
      }
    });
  }

  public void clearItem(long item) {
    AttributeMap map = getAttributeMap(item);
    for (DBAttribute<?> attribute : map.keySet()) {
      setValue(item, attribute, null);
    }
  }

  public long materialize(DBIdentifiedObject object) {
    long m = findMaterialized(object);
    if (m == 0) {
      m = nextItem();
      Log.debug("materializing " + object + "(" + object.getId() + ") as item " + m);
      // important to put before initialize
      setValue(m, DBAttribute.ID, object.getId());
      object.reinitializeItem(m, this);
    }
    return m;
  }

  public void clearAttribute(DBAttribute<?> attribute) {
    try {
      AttributeAdapter adapter = getAttributeAdapter(attribute);
      String table = myContext.getTableName(adapter.getTable(), false);
      if (table == null)
        return;
      SQLiteStatement st = myContext.prepare(myContext.sql().append("DELETE * FROM ").append(table));
      try {
        st.step();
      } finally {
        st.dispose();
      }
      long a = findMaterialized(attribute);
      if (a != 0)
        clearItem(a);
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }
}
