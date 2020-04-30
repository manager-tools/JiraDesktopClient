package com.almworks.util.components;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public abstract class TreeModelAdapter implements TreeModelListener {
  public void treeNodesChanged(TreeModelEvent e) {
    treeModelEvent(e);
  }

  public void treeNodesInserted(TreeModelEvent e) {
    treeModelEvent(e);
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    treeModelEvent(e);
  }

  public void treeStructureChanged(TreeModelEvent e) {
    treeModelEvent(e);
  }

  protected void treeModelEvent(TreeModelEvent e) {
  }
}
