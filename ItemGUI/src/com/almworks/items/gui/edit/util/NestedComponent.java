package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NestedComponent extends BaseComponentControl {
  private final JComponent myOutter;
  private final JComponent myInner;

  public NestedComponent(JComponent outter, JComponent inner, Dimensions dimensions, FieldEditor editor,
    EditItemModel model, Enabled enabled) {
    super(dimensions, editor, model,  editor.getLabelText(model), enabled);
    myOutter = outter;
    myInner = inner;
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myOutter;
  }

  @Override
  protected void enableComponent(boolean enabled) {
    myInner.setEnabled(enabled);
  }

  @Nullable
  public <C extends JComponent> C getInner(Class<C> aClass) {
    return Util.castNullable(aClass, myInner);
  }
}
