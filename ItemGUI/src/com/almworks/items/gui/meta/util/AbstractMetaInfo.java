package com.almworks.items.gui.meta.util;

import com.almworks.api.application.*;
import com.almworks.api.explorer.util.ElementViewerImpl;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.ui.ElementViewer;
import com.almworks.util.ui.UIComponentWrapper2;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public abstract class AbstractMetaInfo implements MetaInfo {
  private final GuiFeaturesManager myFeatures;
  private ToolbarBuilder mySingleToolbarBuilder;
  private ToolbarBuilder mySeveralToolbarBuilder;

  public AbstractMetaInfo(GuiFeaturesManager features) {
    myFeatures = features;
  }

  @Override
  public Collection<LoadedModelKey<?>> getKeys() {
    return getGuiFeatures().getModelKeyCollector().getAllKeys();
  }

  @Override
  public AListModel<ItemsTreeLayout> getTreeLayouts() {
    return getGuiFeatures().getTreeLayouts();
  }

  public final GuiFeaturesManager getGuiFeatures() {
    return myFeatures;
  }

  @Override
  public LongList getSlavesToDiscard(ItemWrapper masterItem) {
    LongArray slaves = new LongArray();
    for (DBStaticObject keyObject : getSlavesToDiscardKeys()) addSlaves(masterItem, slaves, keyObject);
    slaves.sortUnique();
    return slaves;
  }

  private void addSlaves(ItemWrapper masterItem, LongArray target, DBStaticObject listKeyId) {
    LoadedModelKey<List<UiItem>> commentsKey = getGuiFeatures().findListModelKey(listKeyId, UiItem.class);
    List<UiItem> slaves = masterItem.getModelKeyValue(commentsKey);
    if (slaves != null && !slaves.isEmpty()) {
      for (UiItem slave : slaves) target.add(slave.getItem());
    }
  }

  protected abstract List<DBStaticObject> getSlavesToDiscardKeys();

  @Override
  public ModelKey<Boolean> getEditBlockKey() {
    assert false;
    return null;
  }

  @Override
  public <T extends ModelKey<?>> T findKey(String name) {
    assert false;
    return null;
  }

  @Override
  public ElementViewer<ItemUiModel> createViewer(Configuration config) {
    return new ElementViewerImpl(createViewerComponent(config), BasicScalarModel.<JComponent>createConstant(null));
  }

  protected abstract UIComponentWrapper2 createViewerComponent(Configuration config);

  @Override
  public ToolbarBuilder getToolbarBuilder(boolean singleItem) {
    if (singleItem) {
      if (mySingleToolbarBuilder == null) mySingleToolbarBuilder = createToolbar(true);
      return mySingleToolbarBuilder;
    } else {
      if (mySeveralToolbarBuilder == null) mySeveralToolbarBuilder = createToolbar(false);
      return mySeveralToolbarBuilder;
    }
  }

  protected abstract ToolbarBuilder createToolbar(boolean singleArtifact);
}
