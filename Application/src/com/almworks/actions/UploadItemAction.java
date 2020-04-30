package com.almworks.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongList;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class UploadItemAction extends SimpleAction {
  public UploadItemAction() {
    super(L.actionName("&Upload"), Icons.ACTION_COMMIT_ARTIFACT);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Upload changes in selected $(" + Terms.key_artifacts + ") to server"));
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    watchModifiableRole(SyncManager.MODIFIABLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ItemActionUtils.basicUpdate(context, false);

    SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);

    EnableState state = EnableState.DISABLED;
    for(final ItemWrapper wrapper : context.getSourceCollection(ItemWrapper.ITEM_WRAPPER)) {
      final LoadedItem.DBStatus status = wrapper.getDBStatus();
      //noinspection ConstantConditions
      if(status != null && status.isUploadable() && canUpload(syncMan, wrapper)) {
        state = EnableState.ENABLED;
        break;
      }
    }
    context.setEnabled(state);
  }

  private static boolean canUpload(SyncManager syncMan, ItemWrapper wrapper) {
    if (!syncMan.canUpload(wrapper.getItem())) return false;
    LongList items = wrapper.services().getEditableSlaves();
    Connection connection = wrapper.getConnection();
    if (connection == null || !connection.isUploadAllowed()) return false;
    for (int i = 0; i < items.size(); i++) if (!syncMan.canUpload(items.get(i))) return false;
    return true;
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    for(ItemWrapper wrapper : context.getSourceCollection(ItemWrapper.ITEM_WRAPPER)) {
      LoadedItem.DBStatus status = wrapper.getDBStatus();
      //noinspection ConstantConditions
      if(status != null && status.isUploadable() && canUpload(syncMan, wrapper)) {
        Connection connection = wrapper.getConnection();
        assert connection != null : wrapper;
        connection.uploadItems(new LongList.Single(wrapper.getItem()));
      }
    }
  }
}
