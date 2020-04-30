package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.text.NameMnemonic;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class SimpleComponentControl extends BaseComponentControl {
  private final JComponent myComponent;

  public SimpleComponentControl(JComponent component, Dimensions dimension, FieldEditor editor, EditItemModel model,
    Enabled enabled, NameMnemonic label) {
    super(dimension, editor, model, label, enabled);
    myComponent = component;
  }

  public static SimpleComponentControl singleLine(JComponent component, FieldEditor editor, EditItemModel model, Enabled enabled) {
    return create(component, Dimensions.SINGLE_LINE, editor, model, enabled);
  }

  public static SimpleComponentControl create(JComponent component, Dimensions dimension, FieldEditor editor, EditItemModel model,
    Enabled enabled)
  {
    return new SimpleComponentControl(component, dimension, editor, model, enabled, editor.getLabelText(model));
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  protected void enableComponent(boolean enabled) {
    myComponent.setEnabled(enabled);
  }

  public List<ComponentControl> singleton() {
    return Collections.<ComponentControl>singletonList(this);
  }

  public static List<? extends ComponentControl> singleComponent(JComponent component, Dimensions dimension, FieldEditor editor, EditItemModel model, Enabled enabled) {
    return create(component, dimension, editor, model, enabled).singleton();
  }
}
