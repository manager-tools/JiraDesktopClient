package com.almworks.engine.gui.attachments;

import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;

class CopyFileURLAction extends AbstractAttachmentAction {
  public static final AnAction INSTANCE = new CopyFileURLAction();

  private CopyFileURLAction() {
    super("Copy File URL");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    String url = getFileUrl(context);
    context.setEnabled(url != null);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    String url = getFileUrl(context);
    if (url != null) {
      UIUtil.copyToClipboard(url);
    }
  }

  private static String getFileUrl(ActionContext context) throws CantPerformException {
    Attachment attachment = context.getSourceObject(Attachment.ROLE);
    return attachment.getUrl();
  }
}
