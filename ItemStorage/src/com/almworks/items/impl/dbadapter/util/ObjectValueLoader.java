package com.almworks.items.impl.dbadapter.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBReader;
import com.almworks.items.impl.dbadapter.ItemAccessor;
import com.almworks.items.impl.dbadapter.SyncValueLoader;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.Break;
import com.almworks.util.collections.arrays.ObjectArrayAccessor;
import org.jetbrains.annotations.Nullable;

public abstract class ObjectValueLoader<T> implements SyncValueLoader {
  private final ObjectArrayAccessor myAccessor;

  public ObjectValueLoader() {
    this(ObjectArrayAccessor.INSTANCE);
  }

  public ObjectValueLoader(ObjectArrayAccessor accessor) {
    myAccessor = accessor;
  }

  @Nullable
  public T getValue(ItemAccessor item) {
    return (T) item.getValue(this);
  }

  public ObjectArrayAccessor getArrayAccessor() {
    return myAccessor;
  }

  public void load(DBReader reader, LongIterator items, Sink sink) throws SQLiteException, Break {
    Object storage;
    LongArray requested = new LongArray();
    ObjectArrayAccessor accessor = getArrayAccessor();
    while (items.hasNext()) {
      storage = null;
      requested.clear();
      requested.addAllNotMore(items, 40);
      requested.sortUnique();
      for (int i = 0; i < requested.size(); i++) {
        long item = requested.get(i);
        T value = loadValue(reader, item);
        storage = accessor.setObjectValue(storage, i, value);
      }
      if (!requested.isEmpty())
        sink.onLoaded(requested, requested, storage);
    }
  }

  @Nullable
  public abstract T loadValue(DBReader access, long item) throws SQLiteException;
}
