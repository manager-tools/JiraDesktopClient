package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DP;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.Map;

public class ValueMapCache<T> extends PassiveCache {
  private final Map<T, Long> myMap = Collections15.hashMap();

  public ValueMapCache(DatabaseContext databaseContext, BoolExpr<DP> filter, DBAttribute<T> attribute) {
    super(databaseContext, filter, attribute);
  }

  @Override
  protected void beforeUpdate(DBEvent event, TransactionContext context) {
    LongList removed = event.getRemovedAndChangedSorted();
    for (int i = 0; i < removed.size(); i++) {
      long item = removed.get(i);
      T key = (T) getValidated(item, 0);
      if (key == null)
        continue;
      Long removedItem = myMap.remove(key);
      if (removedItem == null) {
        Log.warn(this + " removed null for value " + key);
      }
    }
  }

  @Override
  protected void afterUpdate(DBEvent event, TransactionContext context) {
    LongList items = event.getAddedAndChangedSorted();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      T key = (T) getValidated(item, 0);
      if (key == null)
        continue;
      Long removedItem = myMap.put(key, item);
      if (removedItem != null) {
        Log.debug(this + " item " + item + " expunged " + removedItem + " on value " + key);
      }
    }
  }

  public long getItem(T value, TransactionContext context) throws SQLiteException {
    validate(context);
    Long r = myMap.get(value);
    return r == null ? 0 : r;
  }
}
