package com.almworks.engine.gui.attachments;

import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;

import java.io.File;

class CopyFileLocationAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new CopyFileLocationAction();

  private CopyFileLocationAction() {
    super("Copy File Path");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(getGoodFile(context) != null);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    File file = getGoodFile(context);
    if (file != null) {
      UIUtil.copyToClipboard(file.getAbsolutePath());
    }
  }
}
