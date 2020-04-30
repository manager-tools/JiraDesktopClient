package com.almworks.util.collections;

import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;

import java.util.Arrays;

public class ValueTable {
  private Object[] myCells;
  private int myColumns;
  private int myRows;

  public ValueTable() {
    this(0);
  }

  public ValueTable(int columns) {
    this(columns, 0);
  }

  public ValueTable(int columns, int rows) {
    myColumns = columns;
    myRows = rows;
    int cells = columns * rows;
    myCells = cells > 0 ? new Object[cells] : Const.EMPTY_OBJECTS;
  }

  public Object getCellValue(int column, int row) {
    return myCells[row * myColumns + column];
  }

  public int addRow() {
    myRows++;
    myCells = ArrayUtil.ensureCapacity(myCells, myColumns * myRows);
    return myRows - 1;
  }

  public int getRows() {
    return myRows;
  }

  public void setCell(int column, int row, Object value) {
    myCells[row * myColumns + column] = value;
  }

  public int addColumn() {
    myColumns++;
    myCells = ArrayUtil.ensureCapacity(myCells, myColumns * myRows);
    if (myColumns > 1 && myRows > 1) {
      for (int r = myRows - 1; r > 0; r--) {
        System.arraycopy(myCells, r*(myColumns - 1), myCells, r*myColumns, myColumns - 1);
        myCells[r*myColumns - 1] = null;
      }
    }
    return myColumns - 1;
  }

  public void insertRow(int row) {
    if (row < 0 || row > myRows) {
      LogHelper.error("Out of range", row, myRows);
      return;
    }
    myRows++;
    myCells = ArrayUtil.ensureCapacity(myCells, myColumns * myRows);
    if (myColumns > 0 && myRows != row + 1) {
      System.arraycopy(myCells, row*myColumns, myCells, (row + 1)*myColumns, myColumns*(myRows - row - 1));
      Arrays.fill(myCells, row*myColumns, (row + 1)*myColumns, null);
    }
  }

  public void removeRow(int row) {
    if (row < 0 || row >= myRows) return;
    myRows--;
    if (row < myRows) System.arraycopy(myCells, (row + 1) * myColumns, myCells, row * myColumns, (myRows - row)*myColumns);
    Arrays.fill(myCells, myRows * myColumns, (myRows + 1)*myColumns, null);
  }

  public int getColumns() {
    return myColumns;
  }
}
