package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Lifespan;

import javax.swing.text.JTextComponent;

public abstract class ModelAttachListener {
  private final EditItemModel myModel;
  private boolean myDuringProcess = false;

  public ModelAttachListener(EditItemModel model) {
    myModel = model;
  }

  public ModelAttachListener attachModel(Lifespan life, EditItemModel model) {
    model.addAWTChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        whenModelChanged();
      }
    });
    whenModelChanged();
    return this;
  }

  public ModelAttachListener listenTextComponent(Lifespan life, JTextComponent component) {
    UIUtil.addTextListener(life, component, new ChangeListener() {
      @Override
      public void onChange() {
        whenComponentChanged();
      }
    });
    return this;
  }

  private void whenModelChanged() {
    if (myDuringProcess) return;
    myDuringProcess = true;
    try {
      onModelChanged(myModel);
    } finally {
      myDuringProcess = false;
    }
  }

  protected void whenComponentChanged() {
    if (myDuringProcess) return;
    myDuringProcess = true;
    try {
      onComponentChanged(myModel);
    } finally {
      myDuringProcess = false;
    }
  }

  protected abstract void onModelChanged(EditItemModel model);

  protected abstract void onComponentChanged(EditItemModel model);
}
