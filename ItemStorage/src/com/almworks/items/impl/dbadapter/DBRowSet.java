package com.almworks.items.impl.dbadapter;

import com.almworks.integers.IntArray;
import org.jetbrains.annotations.Nullable;

public class DBRowSet {
  private final DBColumnSet myColumns;

  /**
   * Contains objects created by each column for mass storage
   */
  private final Object[] myColumnStorage;

  private int myRowCount;

  public DBRowSet(DBColumnSet columns) {
    myColumns = columns;
    myColumnStorage = new Object[columns.size()];
  }

  public DBRowSet(DBColumn ... columns) {
    this(DBColumnSet.create(columns));
  }

  public DBColumnSet getColumns() {
    return myColumns;
  }

  public final int getRowCount() {
    return myRowCount;
  }

  public void incRowCount() {
    myRowCount++;
  }

  public String toString() {
    return "RowSet " + getRowCount() + " rows";
  }

  // dbreview should not be used outside dbadapter. remove usages
  @Nullable
  public <T> T getCell(int row, DBColumn<T> column) {
    return getCell(row, myColumns.indexOf(column), column);
  }

  public int getIntCell(int row, DBIntColumn column) {
    int colIndex = myColumns.indexOf(column);
    Object values;
    if (colIndex < 0) {
      assert false : column;
      values = null;
    } else
      values = myColumnStorage[colIndex];
    if (row >= myRowCount) {
      assert false : row + " " + myRowCount;
      row -= myRowCount;
      values = null;
    }
    return column.getArrayAccessor().getIntValue(values, row);
  }
  public long getLongCell(int row, DBLongColumn column) {
    int colIndex = myColumns.indexOf(column);
    Object values;
    if (colIndex < 0) {
      assert false : column;
      values = null;
    } else
      values = myColumnStorage[colIndex];
    if (row >= myRowCount) {
      assert false : row + " " + myRowCount;
      row -= myRowCount;
      values = null;
    }
    return column.getArrayAccessor().getLongValue(values, row);
  }

  private <T> T getCell(int row, int col, DBColumn<T> column) {
    if (col < 0 || col >= myColumnStorage.length) {
      assert false : col + " " + column + " " + myColumns + " " + myColumnStorage;
      return null;
    }
    assert myColumns.get(col) == column : column + " " + col + " " + myColumns;    
    if (row < 0 || row >= myRowCount) {
      assert false : row + " " + myRowCount;
      return null;
    }
    return column.loadValue(myColumnStorage[col], row);
  }

  public int findFirst(DBIntColumn column, int value) {
    int ci = myColumns.indexOf(column);
    if (ci < 0 || ci >= myColumnStorage.length) {
      assert false : column + " " + myColumns + " " + myColumnStorage;
      return -1;
    }
    for (int i = 0; i < getRowCount(); i++)
      if (value == column.getInt(this, i))
        return i;
    return -1;
  }

  public int findFirst(DBRow sample, DBColumnSet ignoreColumns) {
    return findNext(sample, 0, ignoreColumns);
  }

  public int findNext(DBRow sample, int start, DBColumnSet ignoreColumns) {
    DBColumnSet columns = sample.getColumns();
    for (int r = start; r < getRowCount(); r++) {
      boolean found = true;
      for (int c = 0; c < columns.size(); c++) {
        DBColumn column = columns.get(c);
        if (ignoreColumns != null && ignoreColumns.indexOf(column) >= 0)
          continue;
        int ci = myColumns.indexOf(column);
        if (ci < 0)
          return -1;
        if (!column.areEqual(sample.getValue(column), getCell(r, ci, column))) {
          found = false;
          break;
        }
      }
      if (found)
        return r;
    }
    return -1;
  }


  private boolean checkColumnIndex(int ci) {
    if (ci < 0 || ci >= myColumns.size()) {
      assert false;
      return false;
    }
    assert ci < myColumnStorage.length;
    return true;
  }

  public IntArray findAll(DBLongColumn column, long value) {
    return findAll(column, value, null);
  }

  public IntArray findAll(DBLongColumn column, long value, @Nullable IntArray dest) {
    if (dest == null)
      dest = new IntArray();
    for (int i = 0; i < getRowCount(); i++)
      if (value == column.getLong(this, i))
        dest.add(i);
    return dest;
  }

  public DBRow getRow(int row, DBRow dest) {
    if (dest == null)
      dest = DBRowUtil.create(myColumns);
    DBColumnSet destColumns = dest.getColumns();
    for (int i = 0; i < destColumns.size(); i++) {
      DBColumn column = destColumns.get(i);
      dest.setValue(column, getCell(row, column));
    }
    return dest;
  }

  public DBRow getRow(int row, DBRow dest, DBColumn... columns) {
    if (dest == null)
      dest = DBRowUtil.columns(columns);
    for (DBColumn column : columns)
      dest.setValue(column, getCell(row, column));
    return dest;
  }


  Object getColumnData(DBColumn column) {
    int i = myColumns.indexOf(column);
    return getColumnData(i);
  }

  public Object getColumnData(int columnIndex) {
    if (columnIndex < 0 || columnIndex >= myColumnStorage.length) {
      assert false : this + " " + columnIndex;
      return null;
    }
    return myColumnStorage[columnIndex];
  }

  public Object alienateColumnData(DBColumn<?> column) {
    int index = myColumns.indexOf(column);
    if (index < 0) {
      assert false;
      return null;
    }
    Object storage = myColumnStorage[index];
    myColumnStorage[index] = null;
    return storage;
  }

  public void setColumnData(int columnIndex, Object data) {
    if (columnIndex < 0 || columnIndex >= myColumnStorage.length) {
      assert false : this + " " + columnIndex;
      return;
    }
     myColumnStorage[columnIndex] = data;
  }

  public void clear() {
    myRowCount = 0;
  }

  public void copyAllTo(DBRowSet dest) {
    for (int i = 0; i < getRowCount(); i++)
      appendRowTo(i, dest);
  }

  public void appendRowTo(int row, DBRowSet dest) {
    int newRow = dest.getRowCount();
    for (int i = 0; i < myColumns.size(); i++) {
      DBColumn column = myColumns.get(i);
      int destIndex = dest.getColumns().indexOf(column);
      if (destIndex < 0)
        continue;
      Object destStorage = dest.getColumnData(destIndex);
      destStorage = column.appendNative(row, getColumnData(i), newRow, destStorage);
      dest.setColumnData(destIndex, destStorage);
    }
    dest.incRowCount();
  }

  public void copyAllTo(DBRow sample, DBColumnSet ignoreColumns, DBRowSet dest) {
    int row = 0;
    while (row < getRowCount()) {
      row = findNext(sample, row, ignoreColumns);
      if (row < 0)
        return;
      appendRowTo(row, dest);
      row++;
    }
  }

  public boolean hasAllData(int rowIndex, DBRow row) {
    DBColumnSet columns = row.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      int columnIndex = myColumns.indexOf(column);
      if (columnIndex < 0)
        return false;
      if (!column.areEqual(row.getColumnValue(i), getCell(rowIndex, columnIndex, column)))
        return false;
    }
    return true;
  }
}
