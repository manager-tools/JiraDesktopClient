package com.almworks.util.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author Dyoma
 */
public abstract class DocumentAdapter implements DocumentListener {
  public void insertUpdate(DocumentEvent e) {
    documentChanged(e);
  }

  public void removeUpdate(DocumentEvent e) {
    documentChanged(e);
  }

  public void changedUpdate(DocumentEvent e) {
    documentChanged(e);
  }

  protected void documentChanged(DocumentEvent e) {}
}
