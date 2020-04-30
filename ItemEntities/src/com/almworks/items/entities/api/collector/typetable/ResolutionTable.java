package com.almworks.items.entities.api.collector.typetable;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.integers.IntProgression;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValueMerge;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.util.LogHelper;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ResolutionTable {
  private static final Comparator<KeyInfo> COLUMN_ORDER = new Comparator<KeyInfo>() {
    @Override
    public int compare(KeyInfo o1, KeyInfo o2) {
      if (o1 == o2) return 0;
      if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
      KeyInfo.SimpleColumn c1 = Util.castNullable(KeyInfo.SimpleColumn.class, o1);
      KeyInfo.SimpleColumn c2 = Util.castNullable(KeyInfo.SimpleColumn.class, o2);
      if (c1 != null || c2 != null) {
        if (c1 == null || c2 == null) return c1 != null ? -1 : 1;
        int order1 = c1.getClassOrder();
        int order2 = c2.getClassOrder();
        if (order1 != order2) return Util.compareInts(order1, order2);
        else return compareByKey(c1, c2);
      }
      KeyInfo.EntityColumn e1 = Util.castNullable(KeyInfo.EntityColumn.class, o1);
      KeyInfo.EntityColumn e2 = Util.castNullable(KeyInfo.EntityColumn.class, o2);
      LogHelper.assertError(e1 != null && e2 != null, "Unknown column", o1, o2);
      return compareByKey(o1, o2);
    }

    private int compareByKey(KeyInfo c1, KeyInfo c2) {
      return EntityKey.ID_ORDER.compare(c1.getKey(), c2.getKey());
    }
  };

  private final GenericTable myTable;
  private final ResolutionMap myMap;
  private int myHashVersion;

  ResolutionTable(GenericTable table, Collection<EntityKey<?>> keys) {
    myTable = table;
    List<KeyInfo.IdKeyInfo> columns = Collections15.arrayList(keys.size());
    for (EntityKey<?> key : keys) columns.add(createIdColumn(key));
    Collections.sort(columns, COLUMN_ORDER);
    myMap = new ResolutionMap(columns);
    myHashVersion = table.getCollector().getIndexVersion();
  }

  @NotNull
  private KeyInfo.IdKeyInfo createIdColumn(EntityKey<?> key) {
    if (key == null) throw new NullPointerException();
    EntityValueMerge merge = key.toEntity().get(EntityValueMerge.KEY);
    if (merge != null) throw new IllegalArgumentException("Identity merge not allowed " + key + " " + merge);
    KeyInfo.IdKeyInfo result = myTable.getCollector().getOrCreateIdKey(key);
    if (result == null) throw new IllegalArgumentException(key.toString());
    return result;
  }

  @Nullable
  public EntityPlace findRow(ValueRow entity) {
    if (entity == null) return null;
    return priFindRow(entity.getFullRow(myMap.getColumns()));
  }

  @Nullable
  private EntityPlace priFindRow(ValueRow entity) {
    if (entity == null) return null;
    ensureValidHashes();
    int row = myMap.findRow(entity);
    return row >= 0 ? myMap.getValue(row) : null;
  }

  public void addNew(EntityPlace place) {
    ValueRow row = place.getFullRow(myMap.getColumns());
    if (row == null) return;
    priAdd(place, row);
  }

  private void priAdd(EntityPlace place, ValueRow row) {
    ensureValidHashes();
    myMap.put(row, place);
  }

  public boolean hasAllValues(ValueRow entity) {
    ValueRow fullRow = entity.getFullRow(myMap.getColumns());
    if (fullRow == null) return false;
    for (int i = 0; i < fullRow.getColumns().size(); i++) {
      KeyInfo info = fullRow.getColumns().get(i);
      Object value = fullRow.getValue(i);
      if (value == null || value == ValueRow.NULL_VALUE) {
        LogHelper.error("Null identity value", info, entity, myTable);
        return false;
      }
    }
    return true;
  }

  @Nullable
  public EntityPlace update(EntityPlace place) {
    ValueRow row = place.getFullRow(myMap.getColumns());
    if (row == null) return null;
    EntityPlace current = priFindRow(row);
    if (current != null) return current;
    priAdd(place, row);
    return null;
  }

  boolean ensureValidHashes() {
    boolean validateResult = false;
    boolean hashUpdated;
    do {
      hashUpdated = false;
      int targetVersion = myTable.getCollector().getIndexVersion();
      if (myHashVersion == targetVersion) return validateResult;
      ValueRow row = new ValueRow(myTable.getCollector());
      IntArray updated = new IntArray();
      for (int i = 0; i < myMap.size(); i++) {
        EntityPlace resolution = myMap.getValue(i);
        row.setColumns(myMap.getColumns());
        if (!resolution.getFullRow(row)) {
          LogHelper.error("Not full identity detected", row, myMap.getColumns());
          myMap.removeEntry(i);
          i--;
          continue;
        }
        if (!hasChanges(row)) continue;
        updated.add(i);
      }
      if (updated.size() > 0) {
        updated.sortUnique();
        updateIdentities(updated);
        hashUpdated = true;
        validateResult = true;
      }
      myHashVersion = targetVersion;
    } while (hashUpdated);
    return validateResult;
  }

  private void updateIdentities(IntArray updated) {
    ResolutionMap tmpMap = new ResolutionMap(myMap.getColumns());
    TLongLongHashMap replacements = new TLongLongHashMap();
    TLongObjectHashMap<EntityPlace> places = new TLongObjectHashMap<>();
    copyEntries(myMap, tmpMap, updated, replacements, places);
    for (int i = updated.size() - 1; i >= 0; i--) myMap.removeEntry(updated.get(i));
    copyEntries(tmpMap, myMap, IntProgression.arithmetic(0, tmpMap.size()), replacements, places);
    transitiveClosure(replacements);
    for (long oldIndex : replacements.keys()) {
      long newIndex = replacements.get(oldIndex);
      EntityPlace oldPlace = places.get(oldIndex);
      EntityPlace newPlace = places.get(newIndex);
      myTable.merge(newPlace, oldPlace);
    }
  }

  private void copyEntries(ResolutionMap source, ResolutionMap target, IntList entries, TLongLongHashMap replacements, TLongObjectHashMap<EntityPlace> places) {
    ValueRow row = new ValueRow(myTable.getCollector());
    for (IntIterator cursor : entries) {
      int index = cursor.value();
      source.getKey(index, row);
      EntityPlace place = source.getValue(index);
      int knownIndex = target.findRow(row);
      if (knownIndex < 0) target.put(row, place);
      else {
        EntityPlace known = target.getValue(knownIndex);
        ensureMapped(places, place);
        ensureMapped(places, known);
        addReplacement(replacements, place.getIndex(), known.getIndex());
      }
    }
  }

  private void ensureMapped(TLongObjectHashMap<EntityPlace> places, EntityPlace place) {
    EntityPlace existing = places.get(place.getIndex());
    if (existing == null) places.put(place.getIndex(), place);
  }

  private void transitiveClosure(TLongLongHashMap replacements) {
    boolean changed;
    do {
      changed = false;
      for (long key : replacements.keys()) {
        long value = replacements.get(key);
        assert value < key;
        if (!replacements.containsKey(value)) continue;
        long minValue = replacements.get(value);
        assert minValue < value;
        replacements.put(key, minValue);
        changed = true;
      }
    } while (changed);
  }

  private int addReplacement(TLongLongHashMap replacements, int minIndex, int maxIndex) {
    if (minIndex == maxIndex) return minIndex;
    if (minIndex > maxIndex) {
      int tmp = minIndex;
      minIndex = maxIndex;
      maxIndex = tmp;
    }
    assert minIndex < maxIndex;
    if (!replacements.containsKey(maxIndex)) {
      replacements.put(maxIndex, minIndex);
      return minIndex;
    }
    int prev = (int) replacements.get(maxIndex);
    assert prev < maxIndex;
    prev = addReplacement(replacements, minIndex, prev);
    replacements.put(maxIndex, prev);
    return prev;
  }

  private boolean hasChanges(ValueRow row) {
    boolean changed = false;
    List<KeyInfo> columns = row.getColumns();
    for (int j = 0, columnsSize = columns.size(); j < columnsSize; j++) {
      KeyInfo info = columns.get(j);
      KeyInfo.IdKeyInfo idInfo = Util.castNullable(KeyInfo.IdKeyInfo.class, info);
      if (idInfo == null) {
        LogHelper.error("Wrong column", info);
        continue;
      }
      if (idInfo.hasChanges(row.getValue(j), myHashVersion)) {
        changed = true;
        break;
      }
    }
    return changed;
  }

  @Override
  public String toString() {
    return "ResolutionTable [" + myMap.getColumns() + "] rows: " + myMap.size() + " hashVersion: " + myHashVersion;
  }

  public List<KeyInfo> getColumns() {
    return Collections.<KeyInfo>unmodifiableList(myMap.getColumns());
  }

  public List<EntityPlace> getRows() {
    return Collections.unmodifiableList(myMap.getRows());
  }

  public boolean hasColumn(KeyInfo info) {
    return ValueRow.getColumnIndex(myMap.getColumns(), info) >= 0;
  }
}
