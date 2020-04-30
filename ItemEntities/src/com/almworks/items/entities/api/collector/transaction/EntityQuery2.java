package com.almworks.items.entities.api.collector.transaction;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.util.LogHelper;

class EntityQuery2 {
  private final EntityTable myType;
  private final ValueRow myConstraint;

  EntityQuery2(EntityTable type) {
    myType = type;
    myConstraint = new ValueRow(type.getCollector());
  }

  public <T> void addConstraint(EntityKey<T> key, T value) {
    setValue(myType, myConstraint, key, value);
  }

  public void addRefConstraint(EntityKey<Entity> key, EntityHolder value) {
    setReference(myType, myConstraint, key, value);
  }

  EntityTable getTypeTable() {
    return myType;
  }

  static <T> void setValue(EntityTable table, ValueRow row, EntityKey<T> key, T value) {
    KeyInfo info = table.getCollector().getOrCreateKey(key);
    if (info != null) setConvertedValue(table, row, info, info.convertValue(table.getCollector(), value));
  }

  static void setReference(EntityTable table, ValueRow row, EntityKey<Entity> key, EntityHolder value) {
    KeyInfo info = table.getCollector().getOrCreateKey(key);
    if (info != null) setConvertedValue(table, row, info, value != null ? value.getPlace() : ValueRow.NULL_VALUE);
  }

  private static void setConvertedValue(EntityTable table, ValueRow row, KeyInfo info, Object converted) {
    int index = row.getColumnIndex(info);
    if (index < 0) row.addColumn(info, converted);
    else {
      Object prev = row.getValue(index);
      LogHelper.assertError(info.equalValue(converted, prev), "Different values", table, row, prev, converted);
      row.setValue(index, converted);
    }
  }

  public ValueRow getQuery() {
    ValueRow copy = new ValueRow(myType.getCollector());
    copy.setColumns(myConstraint.getColumns());
    for (int i = 0; i < myConstraint.getColumns().size(); i++) {
      copy.setValue(i, myConstraint.getValue(i));
    }
    return copy;
  }
}
