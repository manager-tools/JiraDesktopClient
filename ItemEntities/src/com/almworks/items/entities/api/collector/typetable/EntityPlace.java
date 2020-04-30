package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityPlace {
  public static final EntityPlace[] EMPTY_ARRAY = new EntityPlace[0];
  private final PriEntityTable myTable;
  private int myIndex;
  private int myIndexVersion;
  private Object mySlaves = null;

  EntityPlace(PriEntityTable table, int index) {
    myTable = table;
    myIndex = index;
    myIndexVersion = table.getCollector().getIndexVersion();
  }

  public int getIndex() {
    return myIndex;
  }

  public EntityCollector2 getCollector() {
    return myTable.getCollector();
  }

  @Nullable
  ValueRow getFullRow(List<? extends KeyInfo> columns) {
    ValueRow result = new ValueRow(getCollector());
    result.setColumns(columns);
    return getFullRow(result) ? result : null;
  }

  boolean getFullRow(ValueRow target) {
    List<KeyInfo> columns = target.getColumns();
    int index = getIndex();
    for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
      KeyInfo column = columns.get(i);
      Object value = myTable.getValue(column, index);
      if (value == null) return false;
      target.setValue(i, value);
    }
    return true;
  }

  public EntityTable getTable() {
    return myTable;
  }

  public int getIndexVersion() {
    return myIndexVersion;
  }

  @SuppressWarnings("unchecked")
  void addSlave(EntityPlace other) {
    HashSet<EntityPlace> allSlaves = Collections15.hashSet();
    if (other.isMasterOf(this, allSlaves)) {
      LogHelper.error("Recursive slaves");
      return;
    }
    if (mySlaves == null) mySlaves = other;
    else if (mySlaves.getClass() == ArrayList.class) ((ArrayList<EntityPlace>) mySlaves).add(other);
    else {
      assert mySlaves.getClass() == EntityPlace.class : mySlaves;
      ArrayList<EntityPlace> slaves = new ArrayList<EntityPlace>(4);
      slaves.add((EntityPlace) mySlaves);
      slaves.add(other);
      mySlaves = slaves;
    }
    if (other.getIndex() == myIndex) return;
    int version = getCollector().incIndexVersion();
    other.setIndex(myIndex, version);
  }

  @SuppressWarnings("unchecked")
  private boolean isMasterOf(EntityPlace place, Set<EntityPlace> allSlaves) {
    if (allSlaves.contains(this)) return false;
    allSlaves.add(this);
    if (mySlaves == null) return false;
    else if (mySlaves.getClass() == ArrayList.class) {
      for (EntityPlace slave : ((ArrayList<EntityPlace>) mySlaves)) {
        if (slave == place) return true;
        if (slave.isMasterOf(place, allSlaves)) return true;
      }
      return false;
    }
    else return mySlaves == place || ((EntityPlace) mySlaves).isMasterOf(place, allSlaves);
  }

  @SuppressWarnings("unchecked")
  private void setIndex(int index, int version) {
    if (myIndex == index) return;
    myIndex = index;
    myIndexVersion = version;
    if (mySlaves == null) return;
    if (mySlaves.getClass() == ArrayList.class) for (EntityPlace slave : ((ArrayList<EntityPlace>) mySlaves)) slave.setIndex(index, version);
    else {
      assert mySlaves.getClass() == EntityPlace.class : mySlaves;
      ((EntityPlace) mySlaves).setIndex(index, version);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("EPlace@").append(myIndex).append("(").append(myIndexVersion);
    int currentVersion = getCollector().getIndexVersion();
    if (myIndexVersion != currentVersion) builder.append("/").append(currentVersion);
    builder.append(")").append(":").append(myTable.getItemType().getTypeId());
    HashSet<KeyInfo> columns = Collections15.hashSet();
    for (int i = 0; i < myTable.getResolutionsCount(); i++) columns.addAll(myTable.getResolutionColumns(i));
    KeyInfo[] columnsArray = columns.toArray(new KeyInfo[columns.size()]);
    Arrays.sort(columnsArray, Containers.convertingComparator(KeyInfo.GET_KEY_ID, String.CASE_INSENSITIVE_ORDER));
    if (columnsArray.length > 0) {
      builder.append(" (");
      String sep = "";
      for (KeyInfo info : columnsArray) {
        Object value = getValue(info);
        if (value == null) continue;
        if (value == ValueRow.NULL_VALUE) value = null;
        builder.append(sep);
        sep = ", ";
        builder.append(info.getKey().getId()).append("=").append(value);
      }
      builder.append(")");
    }
    return builder.toString();
  }

  @Nullable
  public KeyInfo getOrCreateColumn(EntityKey<?> key) {
    return getCollector().getOrCreateKey(key);
  }

  public void setConvertedValue(KeyInfo column, Object value) {
    if (value == null) {
      LogHelper.error("Converted value expected", column);
      return;
    }
    if (myTable.setValue(this, column, value, false)) getCollector().mergeIdentities();
  }

  public Object getValue(KeyInfo info) {
    return myTable.getValue(info, myIndex);
  }

  public void setValues(ValueRow row) {
    myTable.setValues(this, row);
  }

  public void override(KeyInfo column, Object value) {
    if (value == null) {
      LogHelper.error("Can not override with NO-VALUE", column);
      return;
    }
    if (myTable.setValue(this, column, value, true)) getCollector().mergeIdentities();
  }

  public void setItem(long item) {
    myTable.setItem(this, item);
  }

  public Entity restoreEntity() {
    return myTable.restoreIdentified(myIndex);
  }
}
