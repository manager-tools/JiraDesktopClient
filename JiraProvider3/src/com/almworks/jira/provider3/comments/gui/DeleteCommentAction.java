package com.almworks.jira.provider3.comments.gui;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.BaseDiscardSlavesAction;
import com.almworks.jira.provider3.gui.actions.DeleteSlavesCommit;
import com.almworks.jira.provider3.gui.viewer.CommentImpl;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.util.LogHelper;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DeleteCommentAction extends BaseDiscardSlavesAction<CommentImpl> {
  public static final AnAction INSTANCE = new DeleteCommentAction();

  private DeleteCommentAction() {
    super("Delete Comment", Icons.ACTION_COMMENT_DELETE, CommentImpl.DATA_ROLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Delete selected comment");
  }

  @Override
  protected void updatePresentation(UpdateContext context, List<CommentImpl> slaves) {
    int count = slaves.size();
    if (count == 1) {
      context.putPresentationProperty(PresentationKey.NAME, "Delete Comment");
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Delete selected comment");
    } else {
      context.putPresentationProperty(PresentationKey.NAME, "Delete " + count + " Comments");
      context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Delete selected comments");
    }
  }

  @Override
  protected boolean canDelete(CommentImpl comment, ActionContext context) {
    if (comment.isLocalNew()) return true;
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
    int permission = comment.getAuthor() == connection.getConnectionUser() ? IssuePermissions.COMMENT_DELETE_OWN : IssuePermissions.COMMENT_DELETE_ALL;
    return IssuePermissions.hasPermission(issue, permission);
  }

  @Override
  protected void confirmAndDelete(ActionContext context, List<CommentImpl> comments) throws CantPerformException {
    JComponent message;
    String windowId;
    if (comments.size() == 1) {
      JPanel panel = new JPanel(UIUtil.createBorderLayout());
      panel.add(BorderLayout.NORTH, new JLabel("Please confirm deletion of this comment:"));
      JTextArea text = new JTextArea(comments.get(0).getText());
      text.setRows(5);
      text.setColumns(50);
      text.setEditable(false);
      panel.add(BorderLayout.CENTER, new JScrollPane(text));
      message = panel;
      windowId = "JIRA.deleteComments.single";
    } else {
      message = new JLabel("Please confirm deletion of " + comments.size() + " comments");
      windowId = "JIRA.deleteComments.multi";
    }
    Boolean upload = ItemActionUtils.askConfirmation(context, message, "Delete Issue Comments", windowId, false); // todo JC-18
    if (upload == null) return;
    DeleteSlavesCommit.perform(comments, context, upload, Comment.ISSUE);
  }
}
