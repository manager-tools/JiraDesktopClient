package com.almworks.api.application;

import com.almworks.api.application.util.ExportDescription;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ElementViewer;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Dyoma
 */
public interface MetaInfo {
  Registry REGISTRY = Registry.REGISTRY;

  Convertor<MetaInfo, AListModel<ItemsTreeLayout>> TREE_LAYOUTS = new Convertor<MetaInfo, AListModel<ItemsTreeLayout>>() {
    public AListModel<ItemsTreeLayout> convert(MetaInfo metaInfo) {
      return metaInfo.getTreeLayouts();
    }
  };

  Collection<? extends ModelKey<?>> getKeys();

  ElementViewer<ItemUiModel> createViewer(Configuration config);

  List<? extends AnAction> getWorkflowActions();

  @Deprecated
  List<? extends AnAction> getActions();

  @Nullable
  ToolbarBuilder getToolbarBuilder(boolean singleItem);

  LongList getSlavesToDiscard(ItemWrapper masterItem);

  ModelKey<Boolean> getEditBlockKey();

  /**
   * Use static model key instance if you know what key you want to get.
   * This method can be useful even in presence of statically available model keys, e.g. for connecting controllers to fields by their names specified in GUI form builder
   */
  <T extends ModelKey<?>> T findKey(String name);

  ExportDescription getExportDescription();

  AListModel<ItemsTreeLayout> getTreeLayouts();

  boolean canImport(ActionContext context, ItemWrapper target, List<ItemWrapper> items) throws
    CantPerformException;

  void importItems(List<ItemWrapper> targets, List<? extends ItemWrapper> items, ActionContext context) throws CantPerformException;

  void acceptFiles(ActionContext context, ItemWrapper item, List<File> fileList) throws CantPerformException;

  void acceptImage(ActionContext context, ItemWrapper issue, Image image) throws CantPerformException;

  String getPartialDownloadHtml();

  String getPartialDownloadShort();

  class Registry {
    private static final Registry REGISTRY = new Registry();

    private final Map<DBItemType, MetaInfo> myMetaInfos =
      new ConcurrentHashMap<DBItemType, MetaInfo>(1);

    public MetaInfo getMetaInfo(long item, DBReader reader) {
      Long kind = DBAttribute.TYPE.getValue(item, reader);
      if (kind == null)
        return null;
      for (Map.Entry<DBItemType, MetaInfo> e : myMetaInfos.entrySet()) {
        if (kind == reader.findMaterialized(e.getKey())) {
          return e.getValue();
        }
      }
      return null;
    }

    public MetaInfo getMetaInfo(ItemVersion version) {
      return getMetaInfo(version.getItem(), version.getReader());
    }

    public void registerMetaInfo(DBItemType type, MetaInfo info) {
      myMetaInfos.put(type, info);
    }
  }
}
