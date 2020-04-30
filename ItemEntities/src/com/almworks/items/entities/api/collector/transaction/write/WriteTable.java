package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.*;

class WriteTable {
  private final DBDrain myDrain;
  private final WriteState myState;
  private final Collection<KeyInfo> myValueColumns;
  private final Collection<KeyInfo> myCreateIdentities;
  private final LongList myItems;
  private final List<EntityPlace> myPlaces;
  private final Map<DBAttribute<?>, IntArray> myClearNotSet;

  private WriteTable(DBDrain drain, WriteState state, LongList items, List<EntityPlace> places, Map<DBAttribute<?>, IntArray> clearNotSet, Collection<KeyInfo> valueColumns,
    ArrayList<KeyInfo> createIdentities) {
    myDrain = drain;
    myState = state;
    myValueColumns = valueColumns;
    myCreateIdentities = createIdentities;
    myItems = items;
    myPlaces = places;
    myClearNotSet = clearNotSet;
  }

  public static void write(WriteState writeState, DBDrain drain, EntityTable table, ResolutionState[] states, Map<Integer, Set<DBAttribute<?>>> clearNotSet) throws DBOperationCancelledException {
    if (states == null || states.length == 0) return;
    LongArray items = new LongArray(states.length);
    List<EntityPlace> places = Collections15.arrayList(states.length);
    for (ResolutionState state : states) {
      long item = state.getItem();
      if (item <= 0) throw new DBOperationCancelledException();
      items.add(item);
      places.add(state.getPlace());
    }
    Map<DBAttribute<?>, IntArray> clearNotSetAttributes = Collections15.hashMap();
    for (Map.Entry<Integer, Set<DBAttribute<?>>> entry : clearNotSet.entrySet()) {
      int placeIndex = entry.getKey();
      for (DBAttribute<?> attribute : entry.getValue()) {
        IntArray placeIndexes = clearNotSetAttributes.get(attribute);
        if (placeIndexes == null) {
          placeIndexes = new IntArray();
          clearNotSetAttributes.put(attribute, placeIndexes);
        }
        placeIndexes.add(placeIndex);
      }
    }
    for (IntArray indexes : clearNotSetAttributes.values()) indexes.sortUnique();
    ArrayList<KeyInfo> allColumns = Collections15.arrayList(table.getAllColumns());
    ArrayList<KeyInfo> createIdentities = filterCreateIdentities(writeState, table, clearNotSetAttributes, allColumns);
    new WriteTable(drain, writeState, items, places, clearNotSetAttributes, allColumns, createIdentities).perform();
  }

  private static ArrayList<KeyInfo> filterCreateIdentities(WriteState writeState, EntityTable table, Map<DBAttribute<?>, IntArray> clearNotSetAttributes,
                                                           ArrayList<KeyInfo> allColumns) {
    AttributeCache cache = writeState.getAttributeCache();
    ArrayList<KeyInfo> createIdentities = Collections15.arrayList();
    for (Iterator<KeyInfo> it = allColumns.iterator(); it.hasNext(); ) {
      KeyInfo info = it.next();
      if (!table.isCreateResolutionColumn(info)) continue;
      if (Boolean.TRUE.equals(info.getKey().toEntity().get(EntityHolder.MUTABLE_IDENTITY))) continue;
      DBAttribute<?> attribute = cache.getCounterpart(info).getAttribute();
      if (attribute == null || cache.isShadowable(attribute)) {
        LogHelper.assertError(attribute == null, "Shadowable creation identity detected", info, table);
        continue;
      }
      it.remove();
      createIdentities.add(info);
      IntArray clear = clearNotSetAttributes.remove(attribute);
      LogHelper.assertError(clear == null, "Clear create identity not allowed", info, table);
    }
    return createIdentities;
  }

  private void perform() {
    AttributeCache cache = myState.getAttributeCache();
    for (KeyInfo identity : myCreateIdentities) {
      KeyCounterpart counterpart = cache.getCounterpart(identity);
      DBAttribute<?> attribute = counterpart.getAttribute();
      if (attribute == null) continue;
      writeIdentity(identity);
    }
    for (KeyInfo column : myValueColumns) {
      KeyCounterpart counterpart = cache.getCounterpart(column);
      DBAttribute<?> attribute = counterpart.getAttribute();
      if (attribute == null || cache.isShadowable(attribute)) continue;
      IntList clearNotSet = Util.NN(myClearNotSet.remove(attribute), IntList.EMPTY);
      writeAttribute(column, clearNotSet);
    }
    clearNotShadowables();
    for (int i = 0, myPlacesSize = myPlaces.size(); i < myPlacesSize; i++) {
      EntityPlace place = myPlaces.get(i);
      long item = myItems.get(i);
      writeShadowables(place, myDrain.changeItem(item));
    }
  }

  private void clearNotShadowables() {
    AttributeCache cache = myState.getAttributeCache();
    for (Iterator<Map.Entry<DBAttribute<?>, IntArray>> it = myClearNotSet.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<DBAttribute<?>, IntArray> entry = it.next();
      DBAttribute<?> attribute = entry.getKey();
      if (cache.isShadowable(attribute)) continue;
      it.remove();
      IntArray indexes = entry.getValue();
      for (int i = 0; i < myPlaces.size(); i++) {
        EntityPlace place = myPlaces.get(i);
        if (indexes.binarySearch(place.getIndex()) < 0) continue;
        long item = myItems.get(i);
        myDrain.changeItem(item).setValue(attribute, null);
      }
    }
  }

  private void writeShadowables(EntityPlace place, ItemVersionCreator item) {
    HashSet<DBAttribute<?>> attributes = Collections15.hashSet();
    AttributeCache cache = myState.getAttributeCache();
    int placeIndex = place.getIndex();
    for (KeyInfo column : myValueColumns) {
      KeyCounterpart counterpart = cache.getCounterpart(column);
      DBAttribute<?> attribute = counterpart.getAttribute();
      if (attribute == null || !cache.isShadowable(attribute)) continue;
      attributes.add(attribute);
      Object columnValue = place.getValue(column);
      if (columnValue == null) {
        IntList indexes = Util.NN(myClearNotSet.get(attribute), IntList.EMPTY);
        if (indexes.binarySearch(placeIndex) >= 0) item.setValue(counterpart.getAttribute(), null);
        continue;
      }
      counterpart.update(myState, item, place);
    }
    for (Map.Entry<DBAttribute<?>, IntArray> entry : myClearNotSet.entrySet()) {
      DBAttribute<?> attribute = entry.getKey();
      if (!cache.isShadowable(attribute)) continue;
      if (attributes.contains(attribute)) continue;
      IntArray indexes = entry.getValue();
      if (indexes.binarySearch(placeIndex) < 0) continue;
      item.setValue(attribute, null);
    }
  }

  private void writeAttribute(KeyInfo column, IntList clearNotSet) {
    KeyCounterpart counterpart = myState.getAttributeCache().getCounterpart(column);
    for (int i = 0; i < myPlaces.size(); i++) {
      EntityPlace place = myPlaces.get(i);
      long item = myItems.get(i);
      Object columnValue = place.getValue(column);
      if (columnValue == null) {
        if (clearNotSet.binarySearch(place.getIndex()) >= 0) myDrain.changeItem(item).setValue(counterpart.getAttribute(), null);
        continue;
      }
      counterpart.update(myState, myDrain.changeItem(item), place);
    }
  }

  private void writeIdentity(KeyInfo column) {
    KeyCounterpart counterpart = myState.getAttributeCache().getCounterpart(column);
    for (int i = 0; i < myPlaces.size(); i++) {
      EntityPlace place = myPlaces.get(i);
      long item = myItems.get(i);
      Object newValue = place.getValue(column);
      if (newValue == null) continue;
      ItemVersionCreator creator = myDrain.changeItem(item);
      Object prevValue = creator.getValue(counterpart.getAttribute());
      if (prevValue != null) LogHelper.assertError(counterpart.equalValue(myState, prevValue, newValue), "Creation identity redefinition", column, prevValue, newValue, place);
      else counterpart.update(myState, creator, place);
    }
  }
}
