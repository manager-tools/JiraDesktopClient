package com.almworks.util.components;

import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.config.Configuration;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ComboFileSelectionField extends BaseFileSelectionField<AComboBox<String>> {
  private final SelectionInListModel<String> myModel = SelectionInListModel.create();
  private final Lifecycle myDisplayable = new Lifecycle(false);
  private AListModel<String> myVariants = AListModel.EMPTY;

  public ComboFileSelectionField() {
    super(new AComboBox<String>());
    getField().setEditable(true);
    getField().setModel(myModel);
    myModel.addSelectionChangeListener(Lifespan.FOREVER, getModifiable());
  }

  public void setVariants(Collection<String> variants) {
    setVariants(FixedListModel.create(variants));
  }

  public void setVariants(AListModel<String> list) {
    if (list == null) list = AListModel.EMPTY;
    if (list == myVariants) return;
    myVariants = list;
    if (isDisplayable()) {
      myDisplayable.cycle();
      myModel.setData(myDisplayable.lifespan(), myVariants);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myDisplayable.cycleStart()) myModel.setData(myDisplayable.lifespan(), myVariants);
  }

  @Override
  public void removeNotify() {
    myDisplayable.cycleEnd();
    super.removeNotify();
  }

  @NotNull
  @Override
  public String getFilename() {
    return Util.NN(getField().getModel().getSelectedItem());
  }

  @Override
  protected void setFilename(@Nullable String filename) {
    getField().getModel().setSelectedItem(filename);
  }

  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myModel.addSelectionChangeListener(life, listener);
  }

  public static class ConfigController {
    private static final String SETTING = "file";
    private final Configuration myConfig;
    private final ComboFileSelectionField myField;

    public ConfigController(Configuration config, ComboFileSelectionField field) {
      myConfig = config;
      myField = field;
    }

    public void configure() {
      List<String> files = myConfig.getAllSettings(SETTING);
      myField.setVariants(files);
    }

    public static ConfigController configure(Configuration config, ComboFileSelectionField field) {
      ConfigController controller = new ConfigController(config, field);
      controller.configure();
      return controller;
    }

    public String getFileName() {
      String filename = myField.getFilename();
      if (filename.isEmpty()) return filename;
      try {
        filename = new File(filename).getCanonicalPath();
      } catch (IOException e) {
        LogHelper.error(e);
        return filename;
      }
      List<String> files = Collections15.arrayList();
      myConfig.getAllSettings(SETTING, files);
      files.remove(filename);
      files.add(0, filename);
      myConfig.setSettings(SETTING, files);
      return filename;
    }
  }
}
