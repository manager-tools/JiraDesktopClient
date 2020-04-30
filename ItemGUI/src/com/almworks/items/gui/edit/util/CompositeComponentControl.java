package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.LogHelper;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class CompositeComponentControl extends BaseComponentControl {
  @Nullable
  private final JComponent myComponent;
  private final ComponentControl[] myComponents;

  public CompositeComponentControl(Dimensions dimensions, FieldEditor editor, EditItemModel model, NameMnemonic label,
    Enabled enabled, @Nullable JComponent component, ComponentControl[] components)
  {
    super(dimensions, editor, model, label, enabled);
    myComponent = component;
    myComponents = ArrayUtil.arrayCopy(components);
  }

  public static List<ComponentControl> single(Dimensions dimensions, FieldEditor editor, EditItemModel model,
    NameMnemonic label, Enabled enabled, JComponent component, ComponentControl... controls)
  {
    return Collections.<ComponentControl>singletonList(
      create(dimensions, editor, model, label, enabled, component, controls));
  }

  public static CompositeComponentControl create(Dimensions dimensions, FieldEditor editor, EditItemModel model,
    NameMnemonic label, Enabled enabled, @Nullable JComponent component, ComponentControl ... controls)
  {
    return new CompositeComponentControl(dimensions, editor, model, label, enabled, component, controls);
  }

  @Override
  protected void enableComponent(boolean enabled) {
    for (ComponentControl component : myComponents) component.setEnabled(enabled);
  }

  @SuppressWarnings( {"NullableProblems"})
  @NotNull
  @Override
  public JComponent getComponent() {
    LogHelper.assertError(myComponent != null, "Should not happen");
    //noinspection ConstantConditions
    return myComponent;
  }
}
