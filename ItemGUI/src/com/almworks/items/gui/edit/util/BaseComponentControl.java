package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.text.NameMnemonic;
import org.jetbrains.annotations.NotNull;

public abstract class BaseComponentControl implements ComponentControl {
  private final Dimensions myDimensions;
  private final FieldEditor myEditor;
  private final EditItemModel myModel;
  private final NameMnemonic myLabel;
  private final Enabled myEnabled;

  public BaseComponentControl(Dimensions dimensions, FieldEditor editor, EditItemModel model, NameMnemonic label,
    Enabled enabled) {
    myDimensions = dimensions;
    myEditor = editor;
    myModel = model;
    myLabel = label;
    myEnabled = enabled;
  }

  @NotNull
  @Override
  public Dimensions getDimension() {
    return myDimensions;
  }

  @Override
  public void setEnabled(boolean enabled) {
    enableComponent(enabled);
    myModel.setEditorEnabled(myEditor, enabled);
  }

  @Override
  public NameMnemonic getLabel() {
    return myLabel;
  }

  protected abstract void enableComponent(boolean enabled);

  @NotNull
  @Override
  public Enabled getEnabled() {
    return myEnabled;
  }
}
