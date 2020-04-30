package com.almworks.util.components;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 * @author dyoma
 */
public abstract class ButtonSelectedListener implements javax.swing.event.ChangeListener {
  private boolean myWasSelected;
  private final JToggleButton myAbsolute;

  protected ButtonSelectedListener(JToggleButton absolute) {
    myAbsolute = absolute;
    myWasSelected = myAbsolute.isSelected();
  }

  public void stateChanged(ChangeEvent e) {
    boolean selected = myAbsolute.isSelected();
    if (myWasSelected != selected && selected)
      selectionChanged();
    myWasSelected = selected;
  }

  protected abstract void selectionChanged();

  public final void attach() {
    myWasSelected = myAbsolute.isSelected();
    myAbsolute.addChangeListener(this);
  }
}
