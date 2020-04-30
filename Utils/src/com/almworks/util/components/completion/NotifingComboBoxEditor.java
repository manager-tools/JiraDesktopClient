package com.almworks.util.components.completion;

import com.almworks.util.collections.Convertor;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.ui.UndoUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionListener;

/**
 * @author dyoma
*/
public class NotifingComboBoxEditor<T> implements ComboBoxEditor {
  private final Convertor<? super T, String> myToString;
  private final CompletingComboBoxController<T> myController;
  private final JTextField myEditor;
  private boolean myFilterUpdate = false;
  private boolean mySetItemUpdate = false;
  private boolean myLastItemValid = false;
  private T myLastItem;

  public NotifingComboBoxEditor(Convertor<? super T, String> toString, CompletingComboBoxController<T> controller, JTextField editor) {
    myController = controller;
    myToString = toString;
    myEditor = editor;
  }

  public Convertor<? super T, String> getToString() {
    return myToString;
  }

  public JTextComponent getTextComponent() {
    return myEditor;
  }

  public void startFilterUpdate() {
    assert !myFilterUpdate;
    myFilterUpdate = true;
  }

  public void stopFilterUpdate() {
    assert myFilterUpdate;
    myFilterUpdate = false;
  }

  public void setItem(Object anObject) {
    if (myFilterUpdate)
      return;
    if (myLastItemValid && myLastItem == anObject) return;
    myLastItem = (T) RecentController.UNWRAPPER.convert(anObject);
    myLastItemValid = true;
    doSetItem(myLastItem);
  }

  private void doSetItem(T item) {
    startSetItem();
    try {
      String str = myToString.convert(item);
      myEditor.setText(str != null ? str : "");
      JTextComponent c = getTextComponent();
      c.setSelectionStart(0);
      int textEnd = getText().length();
      c.setSelectionEnd(textEnd);
      c.setCaretPosition(textEnd);
      UndoUtil.discardUndo(c);
    } finally {
      endSetItem();
    }
  }

  public void setItemText(String text) {
    startSetItem();
    try {
      getTextComponent().setText(text);
    } finally {
      endSetItem();
    }
  }

  private void startSetItem() {
    assert !mySetItemUpdate;
    mySetItemUpdate = true;
  }

  private void endSetItem() {
    assert mySetItemUpdate;
    mySetItemUpdate = false;
  }

  public T getItem() {
    if (!myLastItemValid) {
      myLastItem = myController.getItem(getText());
      myLastItemValid = true;
    }
    return myLastItem;
  }

  public String getText() {
    return getTextComponent().getText();
  }

  void textChanged(DocumentEvent e) {
    if (mySetItemUpdate)
      return;
    myLastItemValid = false;
  }

  public JTextField getEditorComponent() {
    return myEditor;
  }

  public void selectAll() {
    myEditor.selectAll();
    myEditor.requestFocusInWindow();
  }

  public void addActionListener(ActionListener l) {
    myEditor.addActionListener(l);
  }

  public void removeActionListener(ActionListener l) {
    myEditor.removeActionListener(l);
  }

  public boolean isSetItemUpdate() {
    return mySetItemUpdate;
  }
}
