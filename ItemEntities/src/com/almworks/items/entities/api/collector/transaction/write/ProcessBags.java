package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ProcessBags {
  private final WriteState myState;
  private final DBDrain myDrain;
  private final Map<EntityTable, List<EntityBag2>> myByType;

  private ProcessBags(WriteState state, DBDrain drain, Map<EntityTable, List<EntityBag2>> byType) {
    myState = state;
    myDrain = drain;
    myByType = byType;
  }

  public void perform() {
    int totalAll = 0;
    int totalFast = 0;
    long start = System.currentTimeMillis();
    for (Map.Entry<EntityTable, List<EntityBag2>> entry : myByType.entrySet()) {
      BoolExpr<DP> baseQuery = createBaseQuery(entry.getKey().getItemType());
      if (baseQuery == null) continue;
      List<EntityBag2> list = entry.getValue();
      int allSize = list.size();
      totalAll += allSize;
      processEmptyDelete(list, baseQuery);
      totalFast += (allSize - list.size());
      for (EntityBag2 bag : list) processBag(bag, baseQuery);
    }
    LogHelper.debug("Bags all: ", totalAll, " fast: ", totalFast, " time: ", (System.currentTimeMillis() - start));
  }

  private BoolExpr<DP> createBaseQuery(Entity targetType) {
    DBItemType type = getAttributeCache().getType(targetType);
    if (type == null) {
      LogHelper.error("Missing DB type", targetType);
      return null;
    }
    BoolExpr<DP> expr = DPEqualsIdentified.create(DBAttribute.TYPE, type);
    expr = expr.and(DPEquals.create(SyncAttributes.CONNECTION, myState.getConnection()));
    return expr;
  }

  private void processEmptyDelete(List<EntityBag2> list, BoolExpr<DP> baseQuery) {
    ArrayList<EntityBag2> singleDelete = Collections15.arrayList();
    for (Iterator<EntityBag2> it = list.iterator(); it.hasNext(); ) {
      EntityBag2 bag = it.next();
      if (bag.isDelete() && !bag.hasExclusions() && bag.getQuery().getColumns().size() == 1) {
        it.remove();
        singleDelete.add(bag);
      }
    }
    ArrayList<EntityBag2> skipped = Collections15.arrayList();
    for (int i = 0; i < singleDelete.size(); i++) {
      EntityBag2 bag = singleDelete.get(i);
      KeyInfo singleColumn = bag.getQuery().getColumns().get(0);
      singleDelete.remove(i);
      i--;
      List<EntityBag2> same = filterSameEmptyDelete(singleDelete, i + 1, singleColumn.getKey());
      same.add(bag);
      if (!processDeleteSingleColumn(same, baseQuery, getAttributeCache().getCounterpart(singleColumn))) skipped.addAll(same);
    }
    list.addAll(skipped);
  }

  private boolean processDeleteSingleColumn(List<EntityBag2> list, BoolExpr<DP> baseQuery, KeyCounterpart counterpart) {
    ArrayList<Object> values = Collections15.arrayList();
    for (EntityBag2 bag : list) values.add(bag.getQuery().getValue(0));
    BoolExpr<DP> dp = counterpart.queryOneOf(myState, values);
    if (dp == null || dp == BoolExpr.<DP>FALSE()) return false;
    deleteAll(myDrain, myDrain.getReader().query(baseQuery.and(dp)).copyItemsSorted());
    return true;
  }

  private List<EntityBag2> filterSameEmptyDelete(List<EntityBag2> list, int start, EntityKey<?> key) {
    ArrayList<EntityBag2> result = Collections15.arrayList();
    for (int i = start; i < list.size(); i++) {
      EntityBag2 bag = list.get(i);
      List<KeyInfo> columns = bag.getQuery().getColumns();
      if (columns.get(0).getKey().equals(key)) {
        result.add(bag);
        list.remove(i);
        i--;
      }
    }
    return result;
  }

  private void processBag(EntityBag2 bag, BoolExpr<DP> baseQuery) {
    ValueRow query = bag.getQuery();
    BoolExpr<DP> dbQuery = baseQuery;
    for (int i = 0; i < query.getColumns().size(); i++) {
      KeyInfo info = query.getColumns().get(i);
      KeyCounterpart counterpart = getAttributeCache().getCounterpart(info);
      Object queryValue = query.getValue(i);
      BoolExpr<DP> dp = counterpart.query(myState, queryValue);
      if (dp == null) return;
      if (dp == BoolExpr.<DP>FALSE()) return;
      dbQuery = baseQuery.and(dp);
    }
    LongArray items = myDrain.getReader().query(dbQuery).copyItemsSorted();
    for (EntityHolder holder : bag.getExclusions()) {
      long item = myState.getItem(holder.getPlace());
      if (item <= 0) {
        LogHelper.error("Unresolved item", holder);
        return;
      }
      items.remove(item);
    }
    if (items.isEmpty()) return;
    applyBag(myDrain, items, bag);
  }

  private AttributeCache getAttributeCache() {
    return myState.getAttributeCache();
  }

  private void applyBag(DBDrain drain, LongList affected, EntityBag2 bag) {
    if (bag.isDelete()) {
      deleteAll(drain, affected);
      return;
    }
    List<KeyInfo> columns = bag.getChangeColumns();
    if (columns.isEmpty()) {
      LogHelper.error("Nothing to change", affected, bag);
      return;
    }
    for (int i = 0; i < columns.size(); i++) {
      KeyInfo info = columns.get(i);
      Object value = bag.getChangeValue(i);
      if (value == null) {
        LogHelper.error("Empty change value for", info, bag);
        continue;
      }
      KeyCounterpart counterpart = getAttributeCache().getCounterpart(info);
      for (LongIterator cursor : affected) counterpart.update(myState, drain.changeItem(cursor.value()), value);
    }
  }

  private void deleteAll(DBDrain drain, LongList affected) {
    for (LongIterator cursor : affected) drain.changeItem(cursor.value()).delete();
  }

  public static ProcessBags create(WriteState state, DBDrain drain, List<EntityBag2> bags) {
    Map<EntityTable, List<EntityBag2>> byType = collectByType(bags);
    return new ProcessBags(state, drain, byType);
  }

  private static Map<EntityTable, List<EntityBag2>> collectByType(List<EntityBag2> bags) {
    Map<EntityTable, List<EntityBag2>> bagsByType = Collections15.hashMap();
    for (EntityBag2 bag : bags) {
      EntityTable table = bag.getTypeTable();
      List<EntityBag2> list = bagsByType.get(table);
      if (list == null) {
        list = Collections15.arrayList();
        bagsByType.put(table, list);
      }
      list.add(bag);
    }
    return bagsByType;
  }
}
