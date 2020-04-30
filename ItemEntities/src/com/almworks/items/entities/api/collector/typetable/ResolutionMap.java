package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.IntArray;
import com.almworks.util.collections.ValueTable;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.List;

class ResolutionMap {
  private final List<KeyInfo.IdKeyInfo> myColumns;
  private final ValueTable myValueTable;
  private final List<EntityPlace> myRows = Collections15.arrayList();
  private final IntArray myHashes = new IntArray();

  public ResolutionMap(List<KeyInfo.IdKeyInfo> columns) {
    myColumns = columns;
    myValueTable = new ValueTable(myColumns.size());
  }

  public List<KeyInfo.IdKeyInfo> getColumns() {
    return myColumns;
  }

  public EntityPlace getValue(int row) {
    return myRows.get(row);
  }

  public List<EntityPlace> getRows() {
    return myRows;
  }

  public int findRow(ValueRow key) {
    int hash = calcHash(key);
    int row = myHashes.binarySearch(hash);
    if (row < 0) return -1;
    while (row > 0 && myHashes.get(row - 1) == hash) row--;
    while (row < myHashes.size() && myHashes.get(row) == hash) {
      boolean equal = true;
      for (int clm = 0; clm < myColumns.size(); clm++) {
        KeyInfo column = myColumns.get(clm);
        if (!column.equalValue(key.getValue(clm), myValueTable.getCellValue(clm, row))) {
          equal = false;
          break;
        }
      }
      if (equal) return row;
      row++;
    }
    return -1;
  }

  private int calcHash(ValueRow entity) {
    int hash = 0;
    for (int i = 0; i < myColumns.size(); i++) {
      KeyInfo column = myColumns.get(i);
      KeyInfo.IdKeyInfo idColumn = Util.castNullable(KeyInfo.IdKeyInfo.class, column);
      if (idColumn == null) LogHelper.error("Not id column", column);
      else hash += 31*idColumn.getHash(entity.getValue(i));
    }
    return hash;
  }

  public int size() {
    return myRows.size();
  }

  public void removeEntry(int row) {
    myRows.remove(row);
    myHashes.removeAt(row);
    myValueTable.removeRow(row);
  }

  public void put(ValueRow key, EntityPlace value) {
    int hash = calcHash(key);
    int row = myHashes.binarySearch(hash);
    if (row < 0) row = -row -1;
    myHashes.insert(row, hash);
    myValueTable.insertRow(row);
    for (int column = 0, myColumnsSize = myColumns.size(); column < myColumnsSize; column++) myValueTable.setCell(column, row, key.getValue(column));
    myRows.add(row, value);
  }

  public void getKey(int index, ValueRow target) {
    target.setColumns(myColumns);
    for (int i = 0; i < myColumns.size(); i++) target.setValue(i, myValueTable.getCellValue(i, index));
  }

  @Override
  public String toString() {
    return "ResolutionTable[" + myColumns + "] rows: " + myRows.size();
  }
}
