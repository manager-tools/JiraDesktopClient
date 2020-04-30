package com.almworks.util.components.etable;

import javax.swing.*;
import java.util.EventObject;

/**
 * Auxiliary interface representing an editable column
 * in the table. Used by EdiTableManager to coordinate
 * cell traversals.
 */
public interface ColumnEditor {
  /**
   * This is a magic EventObject used to tell the CollectionEditor
   * that an edit was initiated by the EdiTableManager and should
   * be accepted.
   */
  EventObject SHOULD_EDIT_MARKER = new EventObject(Boolean.TRUE);

  /**
   * Set the EdiTableManager coordinating this column.
   * @param manager The manager.
   */
  void setManager(EdiTableManager manager);

  /**
   * Programmatically start cell editing.
   * @param table The table.
   * @param row The (view) row index.
   * @param col The (view) column index.
   */
  void editCell(JTable table, int row, int col);
}
