package com.almworks.engine.gui;

import com.almworks.api.application.DBStatusKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.util.SyncTasksSetUnion;
import com.almworks.api.gui.MainMenu;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.SyncState;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.IconHandle;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.almworks.api.application.ItemWrapper.DBStatus.*;

class LocalChangeMessage implements ChangeListener {
  public static final TypedKey<Object> CONFLICT_KEY = TypedKey.create("conflict");
  private static final TypedKey<Object> LOCAL_CHANGE_KEY = TypedKey.create("localChange");

  public static final String CONFLICT_SHORT = Local.parse("This " + Terms.ref_artifact + " has conflicting changes on the server");
  private static final String CONFLICT_LONG = Local.parse(
    "Upload conflict: This " + Terms.ref_artifact + " was changed on server concurrently with your changes. " +
    "Please resolve the conflict and upload again.");
  private static final String REMOTE_DELETE_SHORT = Local.parse("This " + Terms.ref_artifact + " has been deleted on the server");
  private static final String REMOTE_DELETE_LONG = Local.parse(
    "Upload conflict: This " + Terms.ref_artifact + " was deleted on server concurrently with your changes. " +
      "Please discard your changes.");

  private static final String CHANGED_SHORT = Local.parse("This " + Terms.ref_artifact + " has local changes");
  private static final String CHANGED_LONG = Local.parse(
    "You have modified this " + Terms.ref_artifact + ", but the modifications have not been uploaded to the server yet.");

  private static final String UPLOADING_SHORT = Local.parse("Upload in progress\u2026");
  private static final String UPLOADING_NEW_LONG = Local.parse(
    "This " + Terms.ref_artifact + " is being uploaded to the server.");
  private static final String UPLOADING_OLD_LONG = Local.parse(
    "The modifications you've made to this " + Terms.ref_artifact + " are being uploaded to the server.");

  private static final String NEW_SHORT = Local.parse("This " + Terms.ref_artifact + " is not submitted yet");
  private static final String NEW_LONG = Local.parse(
    "You have created this " + Terms.ref_artifact + ", but have not yet uploaded it to the server.");

  private static final String DISCARDED_SHORT = Local.parse("This " + Terms.ref_artifact + " is discarded");
  private static final String DISCARDED_LONG = Local.parse("You have discarded this " + Terms.ref_artifact + ". It does not exist.");

  private final ModelMap myModel;
  private final ItemMessages myMessages;
  private final boolean myShowDBState;
  private boolean myBeingUploaded;

  public LocalChangeMessage(ModelMap model, ItemMessages messages, boolean showDBState) {
    myModel = model;
    myMessages = messages;
    myShowDBState = showDBState;
  }

  public void attach(Lifespan life) {
    LoadedItemServices itemServices = LoadedItemServices.VALUE_KEY.getValue(myModel);
    if (itemServices == null) return;
    Modifiable syncMan = itemServices.getActor(SyncManager.MODIFIABLE);
    syncMan.addAWTChangeListener(life, this);
    myModel.addAWTChangeListener(life, this);
    attachToSyncTasks(life, itemServices);
    updateMessages();
  }

  private void attachToSyncTasks(final Lifespan life, final LoadedItemServices itemServices) {
    Connection connection = itemServices.getConnection();
    if (connection == null) return;

    long item = itemServices.getItem();
    // todo :refactoring: obtain id from model map somehow (maybe via Connection). Deskzilla can do without it, so it may only be handy for JIRAClient
    Integer serverId = null;
    UploadStatusUpdater uploadStatusUpdater = new UploadStatusUpdater(item, serverId);
    SyncTasksSetUnion.createAwt(uploadStatusUpdater).subscribe(life, connection.getSyncTasks());
  }

  public void onChange() {
    updateMessages();
  }

  private void updateMessages() {
    LoadedItemServices services = LoadedItemServices.VALUE_KEY.getValue(myModel);
    if (services == null) return;
    ItemWrapper.DBStatus status = DBStatusKey.KEY.getValue(myModel);
    updateConflictMessage(services, status);
    updateLocalChangeMessage(services, status);
  }

  private void updateLocalChangeMessage(LoadedItemServices itemServices, ItemWrapper.DBStatus status) {
    if(status != DB_MODIFIED && status != DB_NEW) {
      myMessages.setMessage(LOCAL_CHANGE_KEY, null);
      return;
    }

    final ItemMessage message;
    if(isBeingUploaded(itemServices)) {
      message = ItemMessage.information(Icons.ACTION_COMMITTING_ITEM.getIcon(), UPLOADING_SHORT,
        status == DB_MODIFIED ? UPLOADING_OLD_LONG : UPLOADING_NEW_LONG);
    } else if (myShowDBState) {
      String shortText;
      String longText;
      IconHandle icon = Icons.ARTIFACT_STATE_HAS_UNSYNC_CHANGES;
      if (itemServices.isDeleted()) {
        shortText = DISCARDED_SHORT;
        longText = DISCARDED_LONG;
        icon = Icons.MERGE_STATE_CHANGED_LOCALLY;
      } else if(status == DB_MODIFIED) {
        shortText = CHANGED_SHORT;
        longText = CHANGED_LONG;
      } else {
        shortText = NEW_SHORT;
        longText = NEW_LONG;
      }
      AnAction[] actions = ItemMessage.getActions(itemServices, MainMenu.Edit.UPLOAD, MainMenu.Edit.DISCARD);
      message = ItemMessage.information(icon, shortText, longText, actions);
    } else message = null;
    myMessages.setMessage(LOCAL_CHANGE_KEY, message);
  }

  /**
   * Right after successful upload, returns <tt>true</tt> until the item is reloaded from DB to prevent from showing "has local changes" which can be already not true
   * todo currently it is disabled because there is no guarantee that the item will be reloaded after upload task is finished successully; it can be e.g. if upload found out that server state is the same as the one being uploaded
   */
  private boolean isBeingUploaded(LoadedItemServices itemServices) {
    return myBeingUploaded || ! itemServices.getActor(SyncManager.ROLE).canUpload(itemServices.getItem());
  }

  private void updateConflictMessage(LoadedItemServices itemServices, ItemWrapper.DBStatus status) {
    if (isBeingUploaded(itemServices)) {
      return;
    }
    if (status != DB_CONFLICT) {
      myMessages.setMessage(CONFLICT_KEY, null);
    } else if (!myMessages.hasMessage(CONFLICT_KEY)) {
      String conflictShort;
      String conflictLong;
      if (itemServices.getSyncState() == SyncState.MODIFIED_CORPSE) {
        conflictShort = REMOTE_DELETE_SHORT;
        conflictLong = REMOTE_DELETE_LONG;
      } else {
        conflictShort = CONFLICT_SHORT;
        conflictLong = CONFLICT_LONG;
      }
      myMessages.setMessage(CONFLICT_KEY,
        ItemMessage.synchProblem(conflictShort, conflictLong,
          ItemMessage.getActions(itemServices, MainMenu.Edit.MERGE, MainMenu.Edit.DISCARD)));
    }
  }

  private class UploadStatusUpdater implements Procedure<Collection<SyncTask>> {
    private final long myItem;
    @Nullable
    private final Integer myServerId;

    public UploadStatusUpdater(long item, Integer serverId) {
      myItem = item;
      myServerId = serverId;
    }

    @Override
    public void invoke(Collection<SyncTask> syncTasks) {
      myBeingUploaded = calcBeingUploaded(syncTasks);
      updateMessages();
    }

    private boolean calcBeingUploaded(Collection<SyncTask> syncTasks) {
      for (SyncTask task : syncTasks) {
        if (task.getSpecificActivityForItem(myItem, myServerId) == SyncTask.SpecificItemActivity.UPLOAD && SyncTask.State.isWorking(task.getState().getValue())) {
          return true;
        }
      }
      return false;
    }
  }
}
