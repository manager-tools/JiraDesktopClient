package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public abstract class ComponentHolder extends MockEditor {
  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    return Collections.singletonList(SimpleComponentControl.create(createComponent(life, model), getDimensions(model), this, model, ComponentControl.Enabled.NOT_APPLICABLE));
  }

  protected abstract ComponentControl.Dimensions getDimensions(EditItemModel model);

  protected abstract JComponent createComponent(Lifespan life, EditItemModel model);

  public static FieldEditor createVerticalSpacer(final int height) {
    return new ComponentHolder() {
      @Override
      protected ComponentControl.Dimensions getDimensions(EditItemModel model) {
        return ComponentControl.Dimensions.WIDE_LINE;
      }

      @Override
      protected JComponent createComponent(Lifespan life, EditItemModel model) {
        return new Box.Filler(new Dimension(0, height), new Dimension(0, height), new Dimension(Short.MAX_VALUE, height));
      }
    };
  }
}
