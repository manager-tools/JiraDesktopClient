package com.almworks.util.components;

import com.almworks.util.components.renderer.CellState;

import javax.swing.*;
import java.util.EventObject;

public interface CollectionEditor <T> {
  JComponent getEditorComponent(CellState state, T item);

  /**
   * @return true if editing is allowed
   */
  boolean startEditing(T item);

  /**
   * @param commitEditedData if true, then edited data can be saved, if false - it should be discarded.
   * @return true if editing cannot be stopped (data committed) for some reason, e.g. data validation; if
   *   commitEditedData is false, the returned value has no effect.
   */
  boolean stopEditing(T item, boolean commitEditedData);

  boolean shouldEdit(EventObject event);

  boolean shouldSelect(JTable table, int row, int column, T item);
}
