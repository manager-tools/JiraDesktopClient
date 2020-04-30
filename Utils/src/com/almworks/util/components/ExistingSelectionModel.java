package com.almworks.util.components;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class ExistingSelectionModel extends DefaultListSelectionModel {
  public void insertIndexInterval(int index, int length, boolean before) {
    super.insertIndexInterval(index, length, before);
    if (isSelectionEmpty())
      addSelectionInterval(0, 0);
  }
}
