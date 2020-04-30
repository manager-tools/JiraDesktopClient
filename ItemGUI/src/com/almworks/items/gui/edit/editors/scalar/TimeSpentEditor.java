package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.LogHelper;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class TimeSpentEditor extends DelegatingFieldEditor<ScalarFieldEditor<Integer>> {
  private final TypedKey<SliderController> myControllerKey;
  private final ScalarFieldEditor<Integer> myTextEditor;

  public TimeSpentEditor(NameMnemonic labelText, DBAttribute<Integer> seconds, @Nullable String defaultUnitSuffix) {
    myTextEditor = ScalarFieldEditor.timeInterval(labelText, seconds, defaultUnitSuffix, true);
    myControllerKey = TypedKey.create(seconds.getId() + "/controllerKey");
  }

  @Override
  protected ScalarFieldEditor<Integer> getDelegate(VersionSource source, EditModelState model) {
    return myTextEditor;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    LogHelper.error("No slider created - Not implement yet", getLabelText(model));
    return super.createComponents(life, model);
  }

  public void attach(Lifespan life, EditItemModel model, JTextField field, JSlider slider) {
    ModelWrapper<ScalarFieldEditor<Integer>> wrapper = getWrapperModel(model);
    myTextEditor.attachComponent(life, wrapper, field);
    wrapper.putHint(myControllerKey, SliderController.attach(life, wrapper, slider, myTextEditor, field));
  }

  @Override
  public void afterModelCopied(EditItemModel copy) {
    super.afterModelCopied(copy);
    ModelWrapper<ScalarFieldEditor<Integer>> wrapper = getWrapperModel(copy);
    wrapper.putHint(myControllerKey, null);
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
    super.afterModelFixed(model);
    ModelWrapper<ScalarFieldEditor<Integer>> wrapper = getWrapperModel(model);
    SliderController controller = wrapper.getValue(myControllerKey);
    if (controller != null) controller.saveCurrentValue();
  }

  public Integer getCurrentValue(EditModelState model) {
    return myTextEditor.getCurrentValue(model);
  }

  public void syncValue(Lifespan life, EditItemModel model, Configuration config, String setting, String defaultText) {
    ModelWrapper wrapper = getWrapperModel(model);
    if (wrapper == null) return;
    myTextEditor.syncValue(life, wrapper, config, setting, defaultText);
  }
}
