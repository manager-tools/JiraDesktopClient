package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class DBEntityQuery {
  private final WriteState myState;
  private final BoolExpr<DP> myBaseQuery;
  private final List<KeyInfo> myKeys;

  private DBEntityQuery(WriteState state, DBItemType type, List<KeyInfo> keys) {
    myState = state;
    myBaseQuery = DPEqualsIdentified.create(DBAttribute.TYPE, type).and(DPEquals.create(SyncAttributes.CONNECTION, myState.getConnection()));
    myKeys = keys;
  }
  
  @Nullable
  public static DBEntityQuery create(WriteState state, DBItemType type, List<KeyInfo> columns) {
    return new DBEntityQuery(state, type, columns);
  }

  public long search(EntityPlace place) {
    BoolExpr<DP> query = null;
    for (KeyInfo info : myKeys) {
      KeyCounterpart counterpart = myState.getAttributeCache().getCounterpart(info);
      Object value = place.getValue(info);
      BoolExpr<DP> dp = counterpart.query(myState, value);
      if (dp == null) {
        LogHelper.error("Can not build query", value, info, counterpart);
        return 0;
      }
      if (dp == BoolExpr.<DP>FALSE())
        return 0;
      query = query != null ? query.and(dp) : dp;
    }
    if (query == null) {
      LogHelper.error("No query", myKeys);
      return 0;
    }
    return resolve(query);
  }

  private long resolve(BoolExpr<DP> query) {
    DBReader reader = myState.getReader();
    LongArray result = DatabaseUnwrapper.query(reader, myBaseQuery.and(query)).copyItemsSorted();
    if (result.isEmpty()) return 0;
    if (result.size() == 1) return result.get(0);
    long candidate = 0;
    for (LongIterator cursor : result) {
      long item = cursor.value();
      if (Boolean.TRUE.equals(SyncAttributes.EXISTING.getValue(item, reader))) {
        if (candidate == 0) candidate = item;
        else LogHelper.error("Ambiguous resolution", query, SyncUtils.readTrunk(reader, item), SyncUtils.readTrunk(reader, item));
      }
    }
    return candidate > 0 ? candidate : result.get(0);
  }

  private static long timeToSearch(DBReader reader, BoolExpr<DP> query) {
    long start = System.nanoTime();
    DatabaseUnwrapper.query(reader, query).copyItemsSorted();
    return (System.nanoTime() - start) / 1000;
  }

  private static long timeToLoad(DBReader reader, LongList items, DBAttribute<?> attribute) {
    long start = System.nanoTime();
    attribute.collectValues(items, reader);
    return (System.nanoTime() - start) / 1000;

  }
}
