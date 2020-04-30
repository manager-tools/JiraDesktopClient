package com.almworks.items.entities.api.collector;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.typetable.EntityCollector2;
import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ValueRow {
  /**
   * Null cell value means "no-value", this constant means "null is set".
   */
  public static final Object NULL_VALUE = new Object() {
    @Override
    public String toString() {
      return "v(NULL_VALUE)";
    }
  };
  
  private final EntityCollector2 myCollector;
  private final List<KeyInfo> myColumns = Collections15.arrayList();
  private Object[] myValues = Const.EMPTY_OBJECTS;

  public ValueRow(EntityCollector2 collector) {
    myCollector = collector;
  }

  public List<KeyInfo> getColumns() {
    return myColumns;
  }

  public Object getValue(int index) {
    if (index < 0 || index >= myColumns.size()) {
      LogHelper.error("Out of range", index, myColumns, Arrays.asList(myValues));
    }
    return myValues[index];
  }

  public void clear() {
    setColumns(Collections.<KeyInfo>emptyList());
  }

  public void setColumns(List<? extends KeyInfo> columns) {
    myColumns.clear();
    myColumns.addAll(columns);
    myValues = ArrayUtil.ensureCapacity(myValues, myColumns.size());
    Arrays.fill(myValues, 0, myColumns.size(), null);
  }

  public void copyValues(Entity source) {
    if (source == null) return;
    for (int i = 0, myColumnsLength = myColumns.size(); i < myColumnsLength; i++) {
      KeyInfo info = myColumns.get(i);
      Object value = info.getCellValue(source, myCollector);
      if (value != null) myValues[i] = value;
    }
  }

  public void setValue(int index, @Nullable Object value) {
    myValues[index] = value;
  }

  @Nullable
  public ValueRow getFullRow(List<? extends KeyInfo> columns) {
    ValueRow result = new ValueRow(myCollector);
    result.setColumns(columns);
    for (int dst = 0; dst < columns.size(); dst++) {
      KeyInfo column = columns.get(dst);
      int src = getColumnIndex(column);
      if (src < 0) return null;
      Object value = getValue(src);
      if (value == null) return null;
      result.setValue(dst, value);
    }
    return result;
  }

  public int getColumnIndex(KeyInfo column) {
    return getColumnIndex(myColumns, column);
  }
  
  public static int getColumnIndex(List<? extends KeyInfo> columns, KeyInfo column) {
    if (column == null) return -1;
    return getColumnIndex(columns, column.getKey());
  }

  public static int getColumnIndex(List<? extends KeyInfo> columns, EntityKey<?> key) {
    if (key == null) return -1;
    for (int i = 0; i < columns.size(); i++) {
      KeyInfo info = columns.get(i);
      EntityKey<?> aKey = info.getKey();
      if (key == aKey) return i;
    }
    for (int i = 0; i < columns.size(); i++) {
      KeyInfo info = columns.get(i);
      EntityKey<?> aKey = info.getKey();
      if (aKey.equals(key)) return i;
    }
    assert checkUnknownId(key, columns);
    return -1;
  }

  private static boolean checkUnknownId(EntityKey<?> key, List<? extends KeyInfo> columns) {
    for (KeyInfo info : columns) {
      EntityKey<?> aKey = info.getKey();
      if (aKey.getId().equals(key.getId())) {
        LogHelper.error("Duplicated key ID", aKey, key);
        return true;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (int i = 0, myColumnsSize = myColumns.size(); i < myColumnsSize; i++) {
      Object value = myValues[i];
      if (value == null) continue;
      KeyInfo info = myColumns.get(i);
      if (!first) builder.append(",");
      first = false;
      builder.append(info.getKey().getId()).append("=").append(value == NULL_VALUE ? "<null>" : value);
    }
    return builder.toString();
  }

  public int addColumn(KeyInfo info, Object value) {
    myColumns.add(info);
    myValues = ArrayUtil.ensureCapacity(myValues, myColumns.size());
    int index = myColumns.size() - 1;
    myValues[index] = value;
    return index;
  }

  public ValueRow copyColumnItersection(List<KeyInfo> columns) {
    ValueRow result = new ValueRow(myCollector);
    for (KeyInfo column : columns) {
      int index = getColumnIndex(column);
      if (index < 0) continue;
      Object value = getValue(index);
      if (value != null) result.addColumn(column, value);
    }
    return result;
  }
}
