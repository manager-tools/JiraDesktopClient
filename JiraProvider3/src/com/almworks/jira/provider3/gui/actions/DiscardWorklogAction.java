package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.util.English;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.*;

import java.util.List;

class DiscardWorklogAction extends SimpleAction {
  public static final AnAction INSTANCE = new DiscardWorklogAction();

  private DiscardWorklogAction() {
    super("Discard changes to work log", Icons.ACTION_WORKLOG_ROLLBACK);
    watchRole(LoadedWorklog.WORKLOG);
    watchModifiableRole(SyncManager.MODIFIABLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<LoadedWorklog> worklogs =
      CantPerformException.ensureNotEmpty(context.getSourceCollection(LoadedWorklog.WORKLOG));
    for (LoadedWorklog worklog : worklogs) if (worklog.getSyncState() == SyncState.SYNC) throw new CantPerformException();
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    if (issue.services().isLockedForUpload()) throw new CantPerformException();
    SyncManager manager = context.getSourceObject(SyncManager.ROLE);
    if (manager.findAnyLock(LongArray.create(UiItem.GET_ITEM.collectList(worklogs))) != null) throw new CantPerformException();
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final LongArray worklogs = LongArray.create(
      UiItem.GET_ITEM.collectList(context.getSourceCollection(LoadedWorklog.WORKLOG)));
    final int count = worklogs.size();
    boolean confirmed = DialogsUtil.askConfirmation(context.getComponent(),
      "Would you like to roll back pending changes to " + count + " work log " +
        English.getSingularOrPlural("entry", count) + "?", "Confirm Rollback");
    if (!confirmed) return;
    context.getSourceObject(SyncManager.ROLE).commitEdit(worklogs, new EditCommit() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        for (int i = 0; i < count; i++) drain.discardChanges(worklogs.get(i));
      }

      @Override
      public void onCommitFinished(boolean success) {
      }
    });
  }
}
