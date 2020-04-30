package com.almworks.engine.gui.attachments;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;

import java.io.File;

class SaveAttachmentAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new SaveAttachmentAction();

  private SaveAttachmentAction() {
    super("Save As\u2026");
    watchRole(AttachmentsController.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(AttachmentsController.ROLE);
    context.setEnabled(getGoodFile(context) != null);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    File file = getGoodFile(context);
    if (file != null) {
      context.getSourceObject(AttachmentsController.ROLE).saveAs(file, context.getComponent());
    }
  }
}
