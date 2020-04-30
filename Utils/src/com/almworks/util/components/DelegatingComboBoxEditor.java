package com.almworks.util.components;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class DelegatingComboBoxEditor<T> implements ComboBoxEditor {
  protected final AComboboxModel<T> myModel;

  private final DetachComposite myDetach = new DetachComposite(true);
  private ComboBoxEditor myDelegate;

  protected DelegatingComboBoxEditor(AComboboxModel<T> model) {
    myModel = model;
  }

  protected abstract T createElementFromText(String text);

  protected abstract boolean compareElementWithText(T element, String text);

  protected abstract String getTextFromElement(@NotNull T element);

  public Component getEditorComponent() {
    return getDelegate().getEditorComponent();
  }

  public void setItem(Object object) {
    if (object == null) {
      getDelegate().setItem(null);
    } else {
      String displayName = getTextFromElement((T) object);
      Object item = getDelegate().getItem();
      assert item == null || item instanceof String : item;
      // "if" -- workaround for http://bugs.deskzilla.com/show_bug.cgi?id=124
      if (!displayName.equals(item)) {
        try {
          getDelegate().setItem(displayName);
        } catch (IllegalStateException e) {
          Log.warn("caught ISE when updating editor", e);
          assert false : e;
        }
      }
    }
  }

  public Object getItem() {
    final String text = (String) getDelegate().getItem();
    for (int i = 0; i < myModel.getSize(); i++) {
      T element = myModel.getAt(i);
      if (compareElementWithText(element, text))
        return element;
    }
    return createElementFromText(text);
  }

  public void selectAll() {
    getDelegate().selectAll();
  }

  public void addActionListener(ActionListener l) {
    myDetach.add(UIUtil.addActionListener(getDelegate(), l));
  }

  public void removeActionListener(ActionListener l) {
    getDelegate().removeActionListener(l);
  }

  public Detach attach(AComboBox<T> comboBox) {
    ComboBoxEditor delegate = comboBox.getEditor();
    assert !(delegate instanceof DelegatingComboBoxEditor);
    setDelegate(delegate);
    Detach detach = comboBox.setEditor(this);
    return DetachComposite.create(detach, myDetach);
  }

  protected ComboBoxEditor getDelegate() {
    assert myDelegate != null : this;
    if (myDelegate == null) {
      Log.error("null delegate: " + this);
      return null;
    }
    return myDelegate;
  }

  private void setDelegate(@NotNull ComboBoxEditor delegate) {
    assert myDelegate == null;
    assert delegate != null;
    if (myDelegate != null) {
      Log.error("not null delegate: " + this);
      return;
    }
    myDelegate = delegate;
  }
}
