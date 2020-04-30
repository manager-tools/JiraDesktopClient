package com.almworks.items.gui.edit.engineactions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.gui.WindowController;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.helper.CommitEditHelper;
import com.almworks.items.gui.edit.helper.ConfirmEditDialog;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.ItemEditController;
import com.almworks.util.L;
import com.almworks.util.Pair;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;

import javax.swing.*;

class CommitEditAction extends SimpleAction {
  public static final AnAction SAVE = saveAction();
  public static final AnAction UPLOAD = uploadAction();
  public static final AnAction DISCARD = discardAction();

  private final boolean myUpload;

  private CommitEditAction(String name, Icon icon, boolean upload) {
    super(name, icon);
    myUpload = upload;
    watchModifiableRole(DefaultEditModel.ROLE);
  }

  private static AnAction saveAction() {
    CommitEditAction action = new CommitEditAction(L.actionName(ItemActionUtils.SAVE_NAME), Icons.ACTION_SAVE, false);
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip(Local.parse("Save changes in the local database without uploading to server")));
    return action;
  }

  private static AnAction uploadAction() {
    CommitEditAction action =
      new CommitEditAction(L.actionName(ItemActionUtils.COMMIT_NAME), Icons.ACTION_COMMIT_ARTIFACT, true);
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip(Local.parse("Save changes and upload to server")));
    return action;
  }

  private static AnAction discardAction() {
    OverridePresentation action = new OverridePresentation(WindowController.CLOSE_ACTION) {
      @Override
      public void update(UpdateContext context) throws CantPerformException {
        context.watchModifiableRole(DefaultEditModel.ROLE);
        super.update(context);
        DefaultEditModel.Root model = context.getSourceObject(DefaultEditModel.ROLE);
        if (CommitEditHelper.isLocked(model)) {
          context.putPresentationProperty(PresentationKey.NAME, L.actionName("Close"));
          context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Close window"));
        }
      }
    };
    action.overridePresentation(PresentationKey.SHORTCUT, Shortcuts.ESCAPE);
    action.overridePresentation(PresentationKey.NAME, L.actionName("Cancel"));
    action.overridePresentation(PresentationKey.SMALL_ICON, Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
    action.overridePresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    action.overridePresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Discard changes and close window"));
    return action;
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    try {
      EditDescriptor.DescriptionStrings descriptions = context.getSourceObject(EditDescriptor.DESCRIPTION_STRINGS);
      String description = myUpload ? descriptions.getUploadDescription() :  descriptions.getSaveDescription();
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, description);
    } catch (CantPerformException e) {
      // ignore
    }
    context.getSourceObject(ItemEditController.ROLE);
    EditItemModel model = context.getSourceObject(DefaultEditModel.ROLE);
    boolean locked = CommitEditHelper.isLocked(model);
    context.setEnabled(!locked && model.hasDataToCommit());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    Pair<CommitEditHelper,ConfirmEditDialog.Result> pair = CommitEditHelper.create(context, myUpload);
    CommitEditHelper helper = pair.getFirst();
    if (pair.getSecond() == ConfirmEditDialog.Result.UPLOAD && myUpload)
      helper.addDoneNotification(UploadProcedure.create(context));
    else helper.addDoneNotification(NotUploadedMessage.create(context).asProcedure());
    helper.commit(context);
  }
}
