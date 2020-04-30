package com.almworks.items.gui.edit.helper;

import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.WindowController;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.sync.EditorLock;
import com.almworks.util.L;
import com.almworks.util.LogHelper;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.*;

import static com.almworks.util.ui.DialogsUtil.YES_NO_OPTION;

class DiscardConfirmListener implements AnActionListener {
  private boolean myCallingAction;

  public DiscardConfirmListener() {
    myCallingAction = false;
  }

  @Override
  public void perform(ActionContext context) throws CantPerformException {
    if (myCallingAction) return;
    DefaultEditModel.Root model;
    try {
      model = context.getSourceObject(DefaultEditModel.ROLE);
    } catch (CantPerformException e) {
      LogHelper.error(e, "Missing model");
      model = null;
    }
    EditorLock editorLock;
    try {
      editorLock = context.getSourceObject(EditorContent.EDITOR_LOCK);
    } catch (CantPerformException e) {
      editorLock = null;
    }
    if (editorLock != null && !editorLock.isAlive()) return;
    if (editorLock != null && editorLock.isCommitting()) throw new CantPerformExceptionSilently("During commit");
    if (model == null || !model.isChanged() || CommitEditHelper.isLocked(model)) return;
    String question;
    AnActionListener yesReply;
    AnActionListener noReply;
    if (model.hasDataToCommit()) {
      question = L.content("Would you like to save entered information as a draft?");
      yesReply = new IdActionProxy(MainMenu.NewItem.SAVE_DRAFT);
      noReply = WindowController.CLOSE_ACTION;
    } else {
      question = "Entered values cannot be saved. Are you sure you want to close editor?";
      yesReply = WindowController.CLOSE_ACTION;
      noReply = null;
    }
    context.getSourceObject(WindowController.ROLE).toFront();
    int reply = DialogsUtil.askUser(context.getComponent(), question, L.dialog("Confirm Close Window"), YES_NO_OPTION);
    AnActionListener action;
    switch (reply) {
    case DialogsUtil.YES_OPTION: action = yesReply; break;
    case DialogsUtil.NO_OPTION: action = noReply; break;
    case DialogsUtil.CLOSED_OPTION: action = null; break;
    default: LogHelper.error("Unknown reply", reply); action = null;
    }
    if (action == null) throw new CantPerformExceptionSilently("Close cancelled");
    try {
      myCallingAction = true;
      action.perform(context);
    } finally {
      myCallingAction = false;
    }
  }
}
