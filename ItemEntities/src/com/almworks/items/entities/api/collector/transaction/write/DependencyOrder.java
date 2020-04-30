package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.*;

class DependencyOrder {
  private final DBNamespace myNs;
  private final Map<EntityTable, Set<EntityTable>> myDependency = Collections15.hashMap();
  private final List<EntityTable> myOrder = Collections15.arrayList();
  private final Collection<EntityTable> myAllTables;

  private DependencyOrder(DBNamespace ns, Collection<EntityTable> allTables) {
    myNs = ns;
    myAllTables = allTables;
  }

  public static List<EntityTable> buildResolutionOrder(DBNamespace ns, Collection<EntityTable> allTables) {
    return new DependencyOrder(ns, allTables).perform();
  }
  
  private List<EntityTable> perform() {
    buildDependency();
    buildOrder();
    return myOrder;
  }

  private void buildOrder() {
    int size;
    do {
      size = myOrder.size();
      for (Iterator<Map.Entry<EntityTable,Set<EntityTable>>> it = myDependency.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<EntityTable, Set<EntityTable>> entry = it.next();
        if (entry.getValue().isEmpty()) {
          EntityTable table = entry.getKey();
          myOrder.add(table);
          it.remove();
          for (Set<EntityTable> tables : myDependency.values()) tables.remove(table);
        }
      }
    } while (size < myOrder.size() && !myDependency.isEmpty());
    LogHelper.assertError(myDependency.isEmpty(), myDependency);
  }

  private void buildDependency() {
    for (EntityTable table : myAllTables) {
      Pair<ItemProxy[], EntityPlace[]> pair = table.getResolvedByProxy(myNs);
      IntArray byItem = new IntArray();
      for (EntityPlace place : pair.getSecond()) if (place != null) byItem.add(place.getIndex());
      byItem.sortUnique();
      HashSet<EntityTable> dependency = buildDependency(table, byItem);
      myDependency.put(table, dependency);
    }
  }

  private HashSet<EntityTable> buildDependency(EntityTable table, IntList excludeSorted) {
    HashSet<EntityTable> dependency = Collections15.hashSet();
    for (int i = 0; i < table.getResolutionsCount(); i++) {
      Pair<List<KeyInfo>, Collection<EntityPlace>> resolution = table.getResolution(i);
      List<KeyInfo> columns = resolution.getFirst();
      Collection<EntityPlace> entities = resolution.getSecond();
      buildDependency(columns, entities, dependency, excludeSorted);
    }
    return dependency;
  }

  private void buildDependency(List<KeyInfo> columns, Collection<EntityPlace> entities, HashSet<EntityTable> result, IntList excludeSorted) {
    for (KeyInfo info : columns) {
      KeyInfo.EntityColumn entityColumn = Util.castNullable(KeyInfo.EntityColumn.class, info);
      if (entityColumn == null) {
        LogHelper.assertError(!Entity.class.equals(info.getKey().getValueClass()), "Wrong column class", info);
        continue;
      }
      for (EntityPlace place : entities) {
        if (excludeSorted.binarySearch(place.getIndex()) >= 0) continue;
        EntityPlace valuePlace = getNNEntity(place, entityColumn);
        if (valuePlace == null) continue;
        result.add(valuePlace.getTable());
      }
    }
  }

  private EntityPlace getNNEntity(EntityPlace place, KeyInfo.EntityColumn entityColumn) {
    Object value = place.getValue(entityColumn);
    if (value == null || value == ValueRow.NULL_VALUE) return null;
    EntityPlace valuePlace = Util.castNullable(EntityPlace.class, value);
    LogHelper.assertError(valuePlace != null, "Wrong value class", entityColumn, value);
    return valuePlace;
  }
}
