package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ValueTable;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

class EntityValueTable {
  private final List<KeyInfo> myColumns = Collections15.arrayList();
  private final ValueTable myCells = new ValueTable();
  private final List<KeyInfo> myIdentityKeys;

  public EntityValueTable(List<KeyInfo> identityKeys) {
    myIdentityKeys = Collections15.unmodifiableListCopy(identityKeys);
  }

  public List<KeyInfo> getIdentityKeys() {
    return myIdentityKeys;
  }

  public int addNewRow() {
    return myCells.addRow();
  }

  public boolean mergeRows(int sourceRow, int targetRow) {
    if (sourceRow < 0 || targetRow < 0 || sourceRow >= myCells.getRows() || targetRow >= myCells.getRows()) {
      LogHelper.error("Index out of range", sourceRow, targetRow, myCells.getRows(), myColumns);
      return false;
    }
    boolean changed = false;
    for (int i = 0, myKeysSize = myColumns.size(); i < myKeysSize; i++) {
      Object sourceValue = myCells.getCellValue(i, sourceRow);
      if (sourceValue == null) continue;
      Object targetValue = myCells.getCellValue(i, targetRow);
      KeyInfo info = myColumns.get(i);
      if (targetValue != null) {
        if (info.equalValue(targetValue, sourceValue)) continue;
        reportDifferentValues(sourceValue, targetValue, info);
      }
      myCells.setCell(i, targetRow, sourceValue);
      if (!changed && ValueRow.getColumnIndex(myIdentityKeys, info) >= 0) changed = true;
    }
    return changed;
  }

  private void reportDifferentValues(Object prev, Object update, KeyInfo column) {
    if (!isSignificantChange(prev, update)) return;
    LogHelper.log(isIdentity(column) ? Level.SEVERE : Level.WARNING, new Throwable(), "Different values", column, update, prev, myColumns);
  }

  private boolean isSignificantChange(Object prev, Object update) {
    Date date1 = Util.castNullable(Date.class, prev);
    Date date2 = Util.castNullable(Date.class, update);
    if (date1 != null && date2 != null) {
      long diff = date1.getTime() - date2.getTime();
      return Math.abs(diff) >= Const.SECOND;
    }
    Object[] coll1 = Util.castNullable(Object[].class, prev);
    Object[] coll2 = Util.castNullable(Object[].class, update);
    if (coll1 != null || coll2 != null) {
      boolean bothEmpty = (coll1 == null || coll1.length == 0) && (coll2 == null || coll2.length == 0);
      return !bothEmpty;
    }
    return true;
  }

  public boolean setValue(int row, KeyInfo columnInfo, Object value, boolean override) {
    int column = findOrAddColumn(columnInfo);
    return setCellValue(column, row, value, override);
  }

  public boolean copyValues(int row, ValueRow source, boolean checkIdentityChange) {
    List<KeyInfo> columns = source.getColumns();
    boolean changed = false;
    for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
      KeyInfo info = columns.get(i);
      int column = findOrAddColumn(info);
      Object value = source.getValue(i);
      if (setCellValue(column, row, value, false)) {
        if (!changed && checkIdentityChange && ValueRow.getColumnIndex(myIdentityKeys, info) >= 0) changed = true;
      }
    }
    return changed;
  }
  
  private boolean setCellValue(int column, int row, Object value, boolean override) {
    if (value == null) return false;
    Object prev = myCells.getCellValue(column, row);
    if (prev != null) {
      KeyInfo columnInfo = myColumns.get(column);
      if (columnInfo.equalValue(prev, value)) return false;
      else {
        KeyInfo.HintInfo hintInfo = Util.castNullable(KeyInfo.HintInfo.class, columnInfo);
        if (hintInfo != null) {
          value = hintInfo.mergeValue(prev, value);
          if (hintInfo.equalValue(prev, value)) return false;
        } else if (!override) reportDifferentValues(prev, value, columnInfo);
      }
    }
    myCells.setCell(column, row, value);
    return true;
  }

  private int findOrAddColumn(KeyInfo info) {
    int index = ValueRow.getColumnIndex(myColumns, info);
    if (index >= 0)
      return index;
    myColumns.add(info);
    return myCells.addColumn();
  }

  public Object getValue(KeyInfo column, int row) {
    int index = ValueRow.getColumnIndex(myColumns, column);
    if (index < 0) return null;
    return myCells.getCellValue(index, row);
  }

  public void removeRow(int index) {
    myCells.removeRow(index);
  }

  public int getRowCount() {
    return myCells.getRows();
  }

  public List<KeyInfo> getAllColumns() {
    return Collections.unmodifiableList(myColumns);
  }

  public boolean isIdentity(KeyInfo column) {
    return ValueRow.getColumnIndex(myIdentityKeys, column) >= 0;
  }
}
