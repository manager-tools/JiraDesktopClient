package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class ValueTableTests extends BaseTestCase {

  private ValueTable myTable;

  protected void setUp() throws Exception {
    super.setUp();
    myTable = new ValueTable();
  }

  public void testAddRowAddColumn() {
    myTable.addColumn();
    myTable.addRow();
    checkCell(0, 0, null);
    myTable.setCell(0, 0, "a");
    checkCell(0, 0, "a");
    
    myTable.addRow();
    checkCell(0, 1, null);
    myTable.setCell(0, 1, "b");
    checkCell(0, 0, "a");
    checkCell(0, 1, "b");

    myTable.addColumn();
    checkRow(0, "a", null);
    checkRow(1, "b", null);
  }

  public void testAddColumnAddRow() {
    myTable.addColumn();
    myTable.addRow();
    checkCell(0, 0, null);
    myTable.setCell(0, 0, "a");

    myTable.addColumn();
    checkRow(0, "a", null);
    myTable.setCell(1, 0, "b");
    checkRow(0, "a", "b");

    myTable.addRow();
    checkRow(0, "a", "b");
    checkRow(1, null, null);
    setRow(1, "c", "d");
    checkRow(1, "c", "d");

    myTable.addColumn();
    checkRow(0, "a", "b", null);
    checkRow(1, "c", "d", null);
  }

  public void testInsertRow() {
    myTable.addColumn();
    addRow("a");
    addRow("b");
    addRow("c");
    checkRow(0, "a");
    checkRow(1, "b");
    checkRow(2, "c");
    myTable.insertRow(1);
    checkRow(0, "a");
    checkCell(0, 1, null);
    checkRow(2, "b");
    checkRow(3, "c");
  }

  private void addRow(Object ... cells) {
    int row = myTable.addRow();
    setRow(row, cells);
  }

  private void setRow(int row, Object ... cells) {
    assertEquals(myTable.getColumns(), cells.length);
    for (int i = 0; i < cells.length; i++) myTable.setCell(i, row, cells[i]);
  }

  private void checkRow(int row, @Nullable Object ... expected) {
    int columns = myTable.getColumns();
    expected = Util.NN(expected, Const.EMPTY_OBJECTS);
    assertEquals(columns, expected.length);
    for (int c = 0; c < columns; c++) checkCell(c, row, expected[c]);
  }
  
  private void checkCell(int column, int row, @Nullable Object expected) {
    assertEquals(expected, myTable.getCellValue(column, row));
  }
}
