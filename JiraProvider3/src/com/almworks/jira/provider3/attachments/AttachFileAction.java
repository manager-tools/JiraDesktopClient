package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;

class AttachFileAction extends SimpleAction {
  AttachFileAction() {
    super("Attach Files\u2026", Icons.ACTION_ATTACH_FILE);
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ensureCanAttach(context);
  }

  public static void ensureCanAttach(UpdateContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    CantPerformException.ensure(!issue.services().isRemoteDeleted());
    JiraConnection3 connection = CantPerformException.ensureNotNull(issue.services().getConnection(JiraConnection3.class));
    CantPerformException.ensure(connection.isUploadAllowed());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    JiraAttachments.attachFiles(context, issue, null);
  }
}
