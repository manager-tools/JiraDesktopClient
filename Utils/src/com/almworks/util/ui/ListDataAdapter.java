package com.almworks.util.ui;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author : Dyoma
 */
public abstract class ListDataAdapter implements ListDataListener {
  public void intervalAdded(ListDataEvent e) {
    listModelChanged(e);
  }

  public void intervalRemoved(ListDataEvent e) {
    listModelChanged(e);
  }

  public void contentsChanged(ListDataEvent e) {
    listModelChanged(e);
  }

  protected void listModelChanged(ListDataEvent e) {
  }

  public static abstract class ComboBox extends ListDataAdapter {
    public final void contentsChanged(ListDataEvent e) {
      if (Math.min(e.getIndex0(), e.getIndex1()) == -1)
        selectedItemChanged(e);
      else
        listContentChanged(e);
    }

    protected void listContentChanged(ListDataEvent e) {
      listModelChanged(e);
    }

    protected void selectedItemChanged(ListDataEvent e) {
      listModelChanged(e);
    }
  }
}
