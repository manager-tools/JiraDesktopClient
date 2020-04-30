package com.almworks.util.ui.actions;

import com.almworks.util.ui.ComponentProperty;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseEvent;

/**
 * Implementor of this interface decides whether to perform a default action with a double-click
 */
public interface DoubleClickAuthority {
  ComponentProperty<DoubleClickAuthority> PROPERTY =
    ComponentProperty.createProperty(DoubleClickAuthority.class.getName());

  boolean isDefaultActionAllowed(MouseEvent event, JComponent component);

  class Tree implements DoubleClickAuthority {
    public static final Tree INSTANCE = new Tree();

    public boolean isDefaultActionAllowed(MouseEvent event, JComponent component) {
      if (!(component instanceof JTree))
        return false;
      JTree tree = ((JTree) component);
      int row = tree.getRowForLocation(event.getX(), event.getY());
      if (row == -1)
        return false;
      TreeSelectionModel selectionModel = tree.getSelectionModel();
      if (selectionModel == null)
        return false;
      return selectionModel.isRowSelected(row);
    }
  }
}
