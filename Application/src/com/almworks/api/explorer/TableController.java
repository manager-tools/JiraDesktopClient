package com.almworks.api.explorer;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author dyoma
 */
public interface TableController {
  DataRole<TableController> DATA_ROLE = DataRole.createRole(TableController.class);
  DataRole<JComponent> TABLE_COMPONENT = DataRole.createRole(JComponent.class);
  DataRole<JComponent> ARTIFACT_VIWER_COMPONENT = DataRole.createRole(JComponent.class);
  DataRole<Object> ARTIFACT_VIEW_FOCUS_MARK = DataRole.createRole(Object.class);

  void toggleLifeMode();

  // todo #825 remove it
  @NotNull
  SelectionAccessor<LoadedItem> getSelectedArtifacts();

  void updateAllArtifacts();

  void stopLoading();

  AListModel<? extends LoadedItem> getCollectionModel();

  boolean isContentOutOfDate();

  @Nullable
  String getCollectionShortName();

  @Nullable
  GenericNode getCollectionNode();

  // todo move to widget?
  @NotNull
  List<TableColumnAccessor<LoadedItem, ?>> getSelectedColumns();

  boolean isLoadingDone();

  @NotNull
  MetaInfoCollector getMetaInfoCollector();

  void setTreeLayout(@Nullable ItemsTreeLayout layout);

  void setTreeLayoutById(String layoutId);

  @Nullable
  ItemCollectionContext getItemCollectionContext();

  void setSearchFilterString(String str, boolean regexp, boolean caseSensitive, boolean filter);

  void selectNextHighlighted();

  void selectPrevHighlighted();

  void setHighlightPanelVisible(boolean visible);

  boolean isHighlightPanelVisible();

  public Modifiable getHighlightPanelModifiable();
}
