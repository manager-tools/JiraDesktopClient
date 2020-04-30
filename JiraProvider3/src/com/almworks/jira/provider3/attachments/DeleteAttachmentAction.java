package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.BaseDiscardSlavesAction;
import com.almworks.jira.provider3.gui.actions.DeleteSlavesCommit;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.util.LogHelper;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.UpdateContext;

import java.util.List;

class DeleteAttachmentAction extends BaseDiscardSlavesAction<AttachmentImpl> {

  DeleteAttachmentAction() {
    super("Delete Attachments", null, AttachmentImpl.ROLE);
  }

  @Override
  protected void updatePresentation(UpdateContext context, List<AttachmentImpl> slaves) {
    context.putPresentationProperty(PresentationKey.NAME, slaves.size() == 1 ? "Delete Attachment" : "Delete " + slaves.size() + " Attachments");
  }

  @Override
  protected void confirmAndDelete(ActionContext context, List<AttachmentImpl> attachments) throws CantPerformException {
    String message;
    message = attachments.size() == 1 ?
      "Are you sure you want to delete " + attachments.get(0).getDisplayName() + "?" :
      "Are you sure you want to delete " + attachments.size() + " attachments?";
    if (!DialogsUtil.askConfirmation(context.getComponent(), message, "Delete Attachments")) return;
    DeleteSlavesCommit.perform(attachments, context, false, com.almworks.jira.provider3.schema.Attachment.ISSUE);
  }

  @Override
  protected boolean canDelete(AttachmentImpl attachment, ActionContext context) {
    if (attachment.isLocal()) return true;
    ItemWrapper issue;
    try {
      issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    } catch (CantPerformException e) {
      LogHelper.error(e);
      return false;
    }
    JiraConnection3 connection = issue.services().getConnection(JiraConnection3.class);
    if (connection == null) {
      LogHelper.error("Wrong connection", issue, issue.getConnection());
      return false;
    }
    int permission = attachment.getAuthor() == connection.getConnectionUser() ? IssuePermissions.ATTACHMENT_DELETE_OWN : IssuePermissions.ATTACHMENT_DELETE_ALL;
    return IssuePermissions.hasPermission(issue, permission);
  }
}
