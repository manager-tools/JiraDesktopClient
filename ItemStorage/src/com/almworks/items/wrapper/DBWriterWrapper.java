package com.almworks.items.wrapper;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBWriter;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

/**
 * Note: this class does not extend {@link com.almworks.items.util.DelegatingWriter} intentionally. The aim is to provide a reminder to wrap new methods added to {@link DBWriter}.
 * */
class DBWriterWrapper extends DBReaderWrapper implements DBWriter {
  private final DBWriter myWriter;
  private final Map<DBAttribute<?>, Long> myExistingObjects = Collections15.hashMap();
  private final LongSet myExistingItems = new LongSet();

  DBWriterWrapper(DBWriter writer) {
    super(writer);
    myWriter = writer;
  }

  @Override
  public <T> void setValue(long item, DBAttribute<T> attribute, @Nullable T value) {
    if (!myExistingObjects.containsKey(attribute)) {
      long attr = myWriter.materialize(attribute);
      ensureItemExists(attr);
      myExistingObjects.put(attribute, attr);
    }
    if (!SyncAttributes.EXISTING.equals(attribute)) myWriter.setValue(item, attribute, value);
    else {
      if (Boolean.TRUE.equals(value)) ensureItemExists(item);
      else markNotExists(item);
    }
  }

  private void ensureItemExists(long item) {
    if (myExistingItems.contains(item)) return;
    Boolean value = myWriter.getValue(item, SyncAttributes.EXISTING);
    if (value == null || !value) myWriter.setValue(item, SyncAttributes.EXISTING, true);
    myExistingItems.add(item);
  }

  @Override
  public long nextItem() {
    long item = myWriter.nextItem();
    myWriter.setValue(item, SyncAttributes.EXISTING, true);
    myExistingItems.add(item);
    return item;
  }

  @Override
  public void clearItem(long item) {
    markNotExists(item);
  }

  private void markNotExists(long item) {
    myWriter.setValue(item, SyncAttributes.EXISTING, null);
    myExistingItems.remove(item);
    for (Iterator<Map.Entry<DBAttribute<?>, Long>> it = myExistingObjects.entrySet().iterator(); it.hasNext();) {
      Map.Entry<DBAttribute<?>, Long> entry = it.next();
      if (entry.getValue() == item) {
        it.remove();
        break;
      }
    }
  }

  @Override
  public long materialize(DBIdentifiedObject object) {
    long m = myWriter.materialize(object);
    ensureItemExists(m);
    return m;
  }

  @Override
  public void clearAttribute(DBAttribute<?> attribute) {
    myWriter.clearAttribute(attribute);
  }

  @Override
  public void finallyDo(ThreadGate gate, Procedure<Boolean> procedure) {
    myWriter.finallyDo(gate, procedure);
  }
}
