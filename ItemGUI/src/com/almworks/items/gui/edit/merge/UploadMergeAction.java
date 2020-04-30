package com.almworks.items.gui.edit.merge;

import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.engineactions.NotUploadedMessage;
import com.almworks.items.gui.edit.engineactions.UploadProcedure;
import com.almworks.items.gui.edit.helper.CommitEditHelper;
import com.almworks.items.gui.edit.helper.ConfirmEditDialog;
import com.almworks.util.L;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;

public class UploadMergeAction extends SimpleAction {
  public static final AnAction INSTANCE = new UploadMergeAction();

  public UploadMergeAction() {
    super("Upload", Icons.ACTION_COMMIT_ARTIFACT);
    setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.CTRL_ENTER);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip(Local.parse("Upload merged $(" + Terms.key_artifact + ") to server")));
    watchModifiableRole(DefaultEditModel.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    MergeTableEditor merge = context.getSourceObject(MergeTableEditor.ROLE);
    for (MergeValue value : merge.getMergeValues()) CantPerformException.ensure(!value.isConflict() || value.isResolved());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    Pair<CommitEditHelper,ConfirmEditDialog.Result> pair = CommitEditHelper.create(context, true);
    CommitEditHelper helper = pair.getFirst();
    if (pair.getSecond() == ConfirmEditDialog.Result.UPLOAD)
      helper.addDoneNotification(UploadProcedure.create(context));
    else
      helper.addDoneNotification(NotUploadedMessage.create(context).asProcedure());
    helper.commit(context);
  }
}
