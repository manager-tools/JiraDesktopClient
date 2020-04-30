package com.almworks.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.*;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.Synchronizer;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import java.util.List;

class DownloadItemAction extends SimpleAction {
  private static final Condition<ItemWrapper> CAN_DOWNLOAD = new CanDownload();
  private static final Condition<ItemWrapper> HAS_DETAILS = new HasDetails();

  private static final String DOWNLOAD_SHORT = L.actionName("&Download All Details");
  private static final String RELOAD_SHORT = L.actionName("Reload All &Details");

  private static final String DOWNLOAD_LONG = L.tooltip("Download all details for selected " + Terms.ref_artifacts);
  private static final String RELOAD_LONG = L.tooltip("Re-download all details for selected " + Terms.ref_artifacts);

  private static final String DOWNLOADING = L.actionName("Downloading\u2026");
  private static final String RELOADING = L.actionName("Reloading\u2026");

  private final Synchronizer mySync;
  private boolean myPreToggle;
  private final SimpleModifiable myPreToggleModifiable = new SimpleModifiable();

  public DownloadItemAction(Engine engine) {
    super(L.actionName(DOWNLOAD_SHORT), Icons.ACTION_UPDATE_ARTIFACT);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    mySync = engine.getSynchronizer();
    updateOnChange(mySync.getTasksModifiable());
    updateOnChange(myPreToggleModifiable);
  }

  private static MultiMap<Connection, ItemWrapper> getItemWrappersGroupedByConnection(ActionContext context)
    throws CantPerformException
  {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    MultiMap<Connection, ItemWrapper> result = MultiMap.create();
    for (ItemWrapper wrapper : wrappers) {
      if (wrapper.services().isRemoteDeleted())
        continue;
      Connection connection = wrapper.getConnection();
      if (connection != null && !connection.getState().getValue().isDegrading()) {
        result.add(connection, wrapper);
      }
    }
    return result;
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    MultiMap<Connection, ItemWrapper> map = getItemWrappersGroupedByConnection(context);
    for (Connection connection : map.keySet()) {
      LongList items = PrimitiveUtils.selectThenCollect(CAN_DOWNLOAD, ItemWrapper.GET_ITEM, map.getAll(connection));
      if (!items.isEmpty()) {
        connection.downloadItemDetails(items);
        myPreToggle = true;
      }
    }
    if (myPreToggle) myPreToggleModifiable.fireChanged();
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final List<ItemWrapper> wrappers = ItemActionUtils.basicUpdate(context, false);

    boolean enabled = false;
    String name = DOWNLOAD_SHORT;
    String ongoing = DOWNLOADING;
    String expl = DOWNLOAD_LONG;

    final List<ItemWrapper> updatable = CAN_DOWNLOAD.filterList(wrappers);
    if(!updatable.isEmpty()) {
      enabled = true;

      final List<ItemWrapper> reloadable = HAS_DETAILS.filterList(updatable);
      if(reloadable.size() == updatable.size()) {
        name = RELOAD_SHORT;
        ongoing = RELOADING;
        expl = RELOAD_LONG;
      }
    }

    context.setEnabled(enabled);

    if(areBeingDownloaded(wrappers) || getPreToggledAndClear()) {
      context.putPresentationProperty(PresentationKey.NAME, ongoing);
      context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.ACTION_UPDATING_ITEM.getIcon());
    } else {
      context.putPresentationProperty(PresentationKey.NAME, name);
      context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.ACTION_UPDATE_ARTIFACT);
    }
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, expl);
  }

  public boolean areBeingDownloaded(List<ItemWrapper> wrappers) {
    LongList items = PrimitiveUtils.collect(UiItem.GET_ITEM, wrappers);
  tasks:
    for (SyncTask task : mySync.getTasks().copyCurrent()) {
      if (SyncTask.State.isWorking(task.getState().getValue())) {
        for (LongListIterator i = items.iterator(); i.hasNext();) {
          if (task.getSpecificActivityForItem(i.nextValue(), null) != SyncTask.SpecificItemActivity.DOWNLOAD)
            continue tasks;
        }
        return true;
      }
    }
    return false;
  }

  public boolean getPreToggledAndClear() {
    boolean ret = myPreToggle;
    myPreToggle = false;
    return ret;
  }

  private static class CanDownload extends Condition<ItemWrapper> {
    public boolean isAccepted(ItemWrapper wrapper) {
      if(wrapper == null) {
        return false;
      }
      LoadedItem.DBStatus status = wrapper.getDBStatus();
      return status != LoadedItem.DBStatus.DB_NEW && status != LoadedItem.DBStatus.DB_CONNECTION_NOT_READY;
    }
  }

  private static class HasDetails extends Condition<ItemWrapper> {
    @Override
    public boolean isAccepted(ItemWrapper value) {
      ItemDownloadStage stage = ItemDownloadStageKey.retrieveValue(value);
      return stage == ItemDownloadStage.STALE || stage == ItemDownloadStage.FULL;
    }
  }
}
