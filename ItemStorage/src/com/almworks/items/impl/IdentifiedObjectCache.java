package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.Map;

public class IdentifiedObjectCache extends PassiveCache {
  private static final TypedKey<IdentifiedObjectCache> CACHE = TypedKey.create("identityCache");

  private static final BoolExpr<DP> IDENTIFIED_OBJECTS = DPNotNull.create(DBAttribute.ID);

  private final Map<String, Long> myIdItemMap = Collections15.hashMap();
  private final Map<Long, String> myItemIdMap = Collections15.hashMap();

  public IdentifiedObjectCache(DatabaseContext context) {
    super(context, IDENTIFIED_OBJECTS, DBAttribute.ID);
  }

  public static IdentifiedObjectCache get(TransactionContext context) {
    Map map = context.getSessionContext().getSessionCache();
    IdentifiedObjectCache cache = CACHE.getFrom(map);
    if (cache == null) {
      cache = new IdentifiedObjectCache(context.getDatabaseContext());
      CACHE.putTo(map, cache);
    }
    return cache;
  }

  @Override
  protected void beforeUpdate(DBEvent event, TransactionContext context) {
    LongList removed = event.getRemovedAndChangedSorted();
    for (int i = 0; i < removed.size(); i++) {
      long item = removed.get(i);
      String removedKey = removeItem(item);
      String key = (String) getValidated(item, 0);
      if (!Util.equals(key, removedKey)) {
        Log.warn(this + " had diff keys for " + item + ": " + key + " " + removedKey);
      }
    }
  }

  @Override
  protected void afterUpdate(DBEvent event, TransactionContext context) throws SQLiteException {
    LongList items = event.getAddedAndChangedSorted();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      String key = (String) getValidated(item, 0);
      if (key == null)
        continue;
      Long removedItem = myIdItemMap.put(key, item);
      if (removedItem != null && removedItem != item) {
        Log.warn(this + " item " + item + " expunged " + removedItem + " on value " + key);
        myItemIdMap.remove(removedItem);
      }
      myItemIdMap.put(item, key);
    }
  }

  private String removeItem(long item) {
    String removedId = myItemIdMap.remove(item);
    if (removedId == null) {
      Log.warn(this + " not removed " + item);
    } else {
      Long removedItem = myIdItemMap.remove(removedId);
      if (removedItem == null) {
        Log.warn(this + " not removed " + removedId + " for item " + item);
      } else if (removedItem != item) {
        Log.warn(this + " removed " + removedItem + " for id " + removedId + " for item " + item);
      }
    }
    return removedId;
  }

  public long getMaterialized(DBIdentifiedObject object, TransactionContext context) {
    return getItemById(object.getId(), context);
  }

  public long getItemById(String id, TransactionContext context) {
    try {
      validate(context);
      Long item = myIdItemMap.get(id);
      return item == null ? 0 : item;
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }
}
