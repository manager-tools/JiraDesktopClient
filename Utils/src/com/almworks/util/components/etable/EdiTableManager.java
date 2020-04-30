package com.almworks.util.components.etable;

import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Map;

/**
 * The manager that orchestrates edit traversals in an in-place-editable
 * table. Keeps a reference to the JTable itself, all ColumnEditors and
 * their corresponding model column indexes.
 */
public class EdiTableManager implements KeyListener {
  final JTable myTable;
  final Map<ColumnEditor, Integer> myColIndexes;
  final ColumnEditor myStartEditColumn;
  final boolean myCycleEditing;

  public EdiTableManager(JTable table, Map<ColumnEditor, Integer> columnData) {
    this(table, columnData, null, true);
  }

  /**
   * The constructor that sets up the manager, the table
   * and the editable columns.
   * @param table The managed JTable.
   * @param columnData The map from ColumnEditors to their model column indexes.
   * @param startEditColumn The ColumnEditor that gets the focus when the user presses Enter.
   * @param cycleEditing 
   */
  public EdiTableManager(
    JTable table, Map<ColumnEditor, Integer> columnData,
    ColumnEditor startEditColumn, boolean cycleEditing)
  {
    myTable = table;
    myColIndexes = columnData;
    myStartEditColumn = startEditColumn;
    myCycleEditing = cycleEditing;

    // The columns should make their calls into this instance.
    for(final ColumnEditor ec : myColIndexes.keySet()) {
      ec.setManager(this);
    }

    myTable.setRowSelectionAllowed(true);
    myTable.setColumnSelectionAllowed(false);

    // Suppress JTable's handling of Enter, Left, Right, and Tab keys.
    myTable.addKeyListener(this);
  }

  /**
   * @return The list of this manager's ColumnEditors ordered by
   * their view indexes.
   */
  private List<ColumnEditor> getColumnsOrderedByView() {
    final Map<Integer, ColumnEditor> sortedMap = Collections15.treeMap();
    for(final Map.Entry<ColumnEditor, Integer> e : myColIndexes.entrySet()) {
      sortedMap.put(myTable.convertColumnIndexToView(e.getValue()), e.getKey());
    }

    final List<ColumnEditor> result = Collections15.arrayList();
    for(final Map.Entry<Integer, ColumnEditor> e : sortedMap.entrySet()) {
      result.add(e.getValue());
    }

    return result;
  }

  /**
   * @param ec The original column.
   * @param colDelta The column delta.
   * @return Neighboring column.
   */
  private ColumnEditor getNeighboringColumn(ColumnEditor ec, int colDelta) {
    final List<ColumnEditor> cols = getColumnsOrderedByView();

    int ix = cols.indexOf(ec);
    if(ix < 0) {
      throw new IllegalArgumentException();
    }

    return cols.get(bounds(ix + colDelta, cols.size()));
  }

  private ColumnEditor getStartEditColumn() {
    if(myStartEditColumn != null) {
      return myStartEditColumn;
    }
    return getColumnsOrderedByView().get(0);
  }

  /**
   * @param value The value.
   * @param max The maximum.
   * @return value bounded within [0; max - 1].
   */
  private int bounds(int value, int max) {
    return Math.min(Math.max(value, 0), max - 1);
  }

  /**
   * The real meat of the manager. This method is called
   * by managed columns to commit the current edit and move
   * to some other column.
   * @param ec The column calling this method (the active one).
   * @param rowDelta Row delta, -1, 0, or 1.
   * @param colDelta Column delta, -1, 0, or 1.
   */
  public void moveEditing(ColumnEditor ec, int rowDelta, int colDelta) {
    // One delta must be zero, and the other one must be non-zero.
    assert rowDelta == 0 ^ colDelta == 0;

    final TableCellEditor editor = myTable.getCellEditor();
    final int row = myTable.getEditingRow();
    final int col = myTable.getEditingColumn();
    if(editor == null || row < 0 || col < 0) {
      return;
    }

    editor.stopCellEditing();

    ColumnEditor newEc = ec;
    int newRow = row;
    int newCol = col;

    if(colDelta != 0) {
      newEc = getNeighboringColumn(ec, colDelta);
      if(ec == newEc && myCycleEditing) {
        // If there's no editable column before/after this one,
        // we move one row up/down and to the last/first column.
        newRow = bounds(row + colDelta, myTable.getRowCount());
        if(newRow != row) {
          final List<ColumnEditor> cols = getColumnsOrderedByView();
          newEc = colDelta > 0 ? cols.get(0) : cols.get(cols.size() - 1);
        }
      }
      newCol = myTable.convertColumnIndexToView(myColIndexes.get(newEc));
    }

    if(rowDelta != 0) {
      newRow = bounds(row + rowDelta, myTable.getRowCount());
      if(newRow == row && myCycleEditing) {
        // If there's no editable row before/after this one,
        // me move one column left/right and to the last/first row.
        newEc = getNeighboringColumn(ec, rowDelta);
        if(newEc != ec) {
          newCol = myTable.convertColumnIndexToView(myColIndexes.get(newEc));
          newRow = rowDelta > 0 ? 0 : myTable.getRowCount() - 1;
        }
      }
    }

    if(newRow != row || newCol != col) {
      newEc.editCell(myTable, newRow, newCol);
    } else {
      myTable.requestFocus();
    }
  }

  /* * * KeyListener methods * * */

  public void keyPressed(KeyEvent e) {
    final int code = e.getKeyCode();
    final int mods = e.getModifiers();

    if(code == KeyEvent.VK_ENTER && mods == 0) {
      // Enter in the table starts editing of the row.
      e.consume(); // Default behavior (move down) overridden.
      editSelectedRow(getStartEditColumn());
    } else if((code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_TAB)
        && (mods == 0 || mods == KeyEvent.SHIFT_MASK)) {
      // [Shift-] Left, Right, and Tab in the table are
      // blocked to get rid of the "focused cell" concept.
      // See also in BasicPublishTimeForm.prerender().
      e.consume();
    }
  }

  public void editSelectedRow(ColumnEditor ec) {
    assert myColIndexes.containsKey(ec) : ec;
    final int row = myTable.getSelectedRow();
    if(row >= 0) {
      final int col = myTable.convertColumnIndexToView(myColIndexes.get(ec));
      ec.editCell(myTable, row, col);
    }
  }

  public void keyReleased(KeyEvent e) {}

  public void keyTyped(KeyEvent e) {}
}
