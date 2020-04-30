package com.almworks.items.gui.edit.helper;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CommitEditHelper implements EditCommit {
  /**
   * This flag is set during model commit is in progress
   */
  public static final TypedKey<Boolean> LOCKED_FOR_COMMIT = TypedKey.create("lockedForCommit");
  private LongList myCommittedItems;
  private final EditItemModel myModel;
  private final Collection<FieldEditor> myEditorsToCommit;
  private final List<Procedure<LongList>> myDoneNotifications = Collections15.arrayList();
  private final DefaultEditModel.Root myOriginalModel;
  private final SyncManager myManager;

  private CommitEditHelper(EditItemModel model, Collection<FieldEditor> commitEditors, DefaultEditModel.Root originalModel, SyncManager manager) {
    myModel = model;
    myEditorsToCommit = commitEditors;
    myOriginalModel = originalModel;
    myManager = manager;
  }

  public static Pair<CommitEditHelper, ConfirmEditDialog.Result> create(ActionContext context, boolean upload) throws CantPerformException {
    DefaultEditModel.Root model = context.getSourceObject(DefaultEditModel.ROLE);
    DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    DataVerification errors = model.verifyData(DataVerification.Purpose.PRE_COMMIT_NOTIFICATION);
    ConfirmEditDialog.Result result;
    if (!errors.hasErrors()) result = upload ? ConfirmEditDialog.Result.UPLOAD : ConfirmEditDialog.Result.SAVE_LOCALLY;
    else result = confirmEditErrors(dialogManager, upload, errors);
    return Pair.create(create(syncManager, model), result);
  }

  public static CommitEditHelper create(SyncManager syncManager, DefaultEditModel.Root model) {
    Collection<FieldEditor> changed = model.getCommitEditors();
    EditItemModel copy = model.copyState();
    return new CommitEditHelper(copy, changed, model, syncManager);
  }

  private static ConfirmEditDialog.Result confirmEditErrors(DialogManager dialogManager, boolean upload, DataVerification verification) throws CantPerformException {
    DialogBuilder dialog = dialogManager.createBuilder("createArtifact.confirmEdit");
    ConfirmEditDialog.Result res = ConfirmEditDialog.show(dialog, upload, verification.getErrorMessage("\n"), false);
    if (res.isContinueEdit()) {
      focusError(verification);
      throw new CantPerformExceptionSilently("User chose to continue editing");
    }
    return res;
  }

  private static void focusError(DataVerification verification) {
    List<DataVerification.Problem> errors = verification.getErrors();
    Set<Component> focusable = Collections15.hashSet();
    for (DataVerification.Problem error : errors) {
      List<JComponent> components = FieldEditorUtil.getRegisteredComponents(error.getModel(), error.getEditor());
      if (components.isEmpty()) continue;
      for (JComponent component : components) {
        if (component.isFocusable()) focusable.add(component);
        for (Component comp : SwingTreeUtil.descendants(component)) if (comp.isFocusable()) focusable.add(comp);
      }
    }
    if (focusable.isEmpty()) return;
    Component[] array = focusable.toArray(new Component[focusable.size()]);
    for (Component component : array) {
      if (!focusable.contains(component)) continue;
      for (Iterator<Component> it = focusable.iterator(); it.hasNext(); ) {
        Component next = it.next();
        if (next == component) continue;
        if (SwingTreeUtil.isAncestor(next, component)) it.remove();
      }
    }
    for (Component component : focusable) if (component.requestFocusInWindow()) break;
  }

  public static boolean isLocked(EditItemModel model) {
    return Boolean.TRUE.equals(model.getValue(LOCKED_FOR_COMMIT));
  }

  public void addDoneNotification(Procedure<LongList> notification) {
    synchronized (myDoneNotifications) {
      myDoneNotifications.add(notification);
    }
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    if (myModel.isNewItem()) {
      ItemCreator creator = myModel.getValue(ItemCreator.KEY);
      if (creator == null) {
        LogHelper.error("Missing creator");
        throw new DBOperationCancelledException();
      }
      ItemVersionCreator item = drain.createItem();
      creator.setupNewItem(myModel, item);
      CommitContext.CommitRoot root = CommitContext.createRoot(myManager, drain, myModel, item);
      for (FieldEditor editor : myEditorsToCommit)
        try {
          editor.commit(root.getRootContext());
        } catch (CancelCommitException e) {
          LogHelper.error("Commit cancelled", item, editor, e);
          throw new DBOperationCancelledException(); // Whole commit should be canceled since there is nothing to write to DB
        }
      root.finishCommit();
      myCommittedItems = LongArray.create(item.getItem());
    } else {
      LongList items = myModel.getEditingItems();
      List<CommitContext.CommitRoot> toFinish = Collections15.arrayList();
      for (ItemVersionCreator item : drain.changeItems(items)) {
        CommitContext.CommitRoot root = CommitContext.createRoot(myManager, drain, myModel, item);
        boolean cancelled = false;
        for (FieldEditor editor : myEditorsToCommit)
          try {
            editor.commit(root.getRootContext());
          } catch (CancelCommitException e) {
            cancelled = true;
            if (root.canCancel()) break;
            else {
              LogHelper.error("Commit cancelled", item, editor, e);
              throw new DBOperationCancelledException();
            }
          }
        if (!cancelled) toFinish.add(root);
      }
      for (CommitContext.CommitRoot root : toFinish) root.finishCommit();
      myCommittedItems = items;
    }
  }

  @Override
  public void onCommitFinished(boolean success) {
    Procedure<LongList>[] array;
    synchronized (myDoneNotifications) {
      array = myDoneNotifications.toArray(new Procedure[myDoneNotifications.size()]);
    }
    for (Procedure<LongList> procedure : array) procedure.invoke(success ? myCommittedItems : null);
  }

  public void commit(ActionContext context) throws CantPerformException {
    ItemEditController controller = context.getSourceObject(ItemEditController.ROLE);
    controller.commit(context, this);
    UnlockModel.install(this);
  }

  private static class UnlockModel implements Procedure<LongList>, Runnable {
    private final DefaultEditModel.Root myModel;

    public UnlockModel(DefaultEditModel.Root model) {
      myModel = model;
    }

    public static void install(CommitEditHelper helper) {
      DefaultEditModel.Root model = helper.myOriginalModel;
      model.putValue(LOCKED_FOR_COMMIT, Boolean.TRUE);
      helper.addDoneNotification(new UnlockModel(model));
    }

    @Override
    public void invoke(LongList arg) {
      ThreadGate.AWT.execute(this);
    }

    @Override
    public void run() {
      myModel.putValue(LOCKED_FOR_COMMIT, null);
    }
  }
}
