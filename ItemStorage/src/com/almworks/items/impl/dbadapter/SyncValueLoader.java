package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.Break;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;

public interface SyncValueLoader {
  NullableArrayStorageAccessor getArrayAccessor();

  void load(DBReader reader, LongIterator items, Sink sink) throws SQLiteException, Break;

  interface Sink {
    /**
     * @param requestedItems sorted and unique
     * @param loadedItems sorted and unique
     * @param data
     * @throws Break
     * @throws SQLiteException
     */
    void onLoaded(LongList requestedItems, LongList loadedItems, Object data) throws Break, SQLiteException;
  }
}
