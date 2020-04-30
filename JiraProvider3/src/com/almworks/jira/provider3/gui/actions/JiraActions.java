package com.almworks.jira.provider3.gui.actions;

import com.almworks.actions.console.actionsource.ActionGroup;
import com.almworks.actions.console.actionsource.ConsoleActionsComponent;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.gui.MainMenu;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.items.gui.edit.engineactions.NewItemAction;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.visibility.FieldVisibilityController;
import com.almworks.jira.provider3.attachments.AttachmentImpl;
import com.almworks.jira.provider3.attachments.AttachmentNameTopEditor;
import com.almworks.jira.provider3.attachments.AttachmentsEditor;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.comments.gui.BaseEditComment;
import com.almworks.jira.provider3.comments.gui.DeleteCommentAction;
import com.almworks.jira.provider3.custom.customize.EditCustomFieldTypesAction;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.actions.copyissue.CustomCopyAction;
import com.almworks.jira.provider3.gui.edit.editors.EditWatchersFeature;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.jira.provider3.gui.timetrack.edit.CreateWorklogFeature;
import com.almworks.jira.provider3.gui.timetrack.edit.DeleteWorklogFeature;
import com.almworks.jira.provider3.gui.timetrack.edit.EditWorklogsFeature;
import com.almworks.jira.provider3.gui.viewer.CommentImpl;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.issue.features.move.MoveIssueFeature;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.jira.provider3.links.actions.AddLinksFeature;
import com.almworks.jira.provider3.links.actions.LinksEditor;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.DelegateToLocalAction;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;

public class JiraActions {
  public static final java.util.List<String> RIGHT_ACTION_IDS =
    Collections15.unmodifiableListCopy(FieldVisibilityController.FIELD_VISIBILITY_ACTION);

  public static final String CREATE_SUBTASK = "Jira.Edit.CreateSubtask";
  public static final String ADD_COMMENT = "Jira.Edit.AddComment";
  public static final String EDIT_COMMENT = "Jira.Edit.EditComment";
  public static final String REPLY_TO_COMMENT = "Jira.Edit.ReplyToComment";
  public static final String DELETE_COMMENT = "Jira.Edit.DeleteComment";
//  public static final String MERGE_RESOLVE_COMMENT = "Jira.Merge.Comments.ResolveComment";

  public static final String VOTE_FOR_ISSUE = "Jira.Edit.VoteForIssue";
  public static final String WATCH_ISSUE = "Jira.Edit.WatchIssue";
  public static final String EDIT_WATCHERS = "Jira.Edit.EditWatchers";

  public static final String REMOVE_LINKS = "Jira.Edit.RemoveLinks";
  public static final String VIEW_ALL_LINKED_ISSUES = "Jira.Link.ViewAllLinked";
  public static final String VIEW_LINKED_ISSUES = "Jira.Link.ViewLinked";
  public static final String ADD_LINKS = "Jira.Edit.AddLinks";
  public static final String LINK_TWO_ISSUES = "Jira.Edit.LinkTwoIssues";
  public static final String EDITOR_ADD_LINKS = "Jira.Link.Editor.AddLinks";
  public static final String EDITOR_REMOVE_LINKS = "Jira.Link.Editor.RemoveLinks";
  public static final String EDITOR_ATTACH_FILES = "Jira.Attachments.Editor.AttachFiles";
  public static final String EDITOR_ATTACH_SCREENSHOT = "Jira.Attachments.Editor.AttachScreenShot";
  public static final String EDITOR_ATTACH_TEXT = "Jira.Attachments.Editor.AttachText";
  public static final String EDITOR_DELETE_ATTACHMENT = "Jira.Attachments.Editor.Delete";
  public static final String EDITOR_RENAME_ATTACHMENT = "Jira.Attachments.Editor.Rename";

  public static final String LOG_WORK = "Jira.TimeTracking.LogWork";
  public static final String LOG_WORK_EDIT = "Jira.TimeTracking.EditWorklog";
  public static final String LOG_WORK_DELETE = "Jira.TimeTracking.DeleteWorklog";
  public static final String LOG_WORK_ROLLBACK = "Jira.TimeTracking.RollbackWorklog";

  public static final String MOVE_ISSUE = "Jira.Edit.MoveIssue";
  public static final String ASSIGN_TO = "Jira.Edit.AssignTo";

  public static final String ATTACH_FILE = "Jira.Attachments.AttachFile";
  public static final String ATTACH_SCREEN_SHOT = "Jira.Attachments.AttachScreenShot";
//  public static final String EDIT_IMAGE_ATTACHMENT = "Jira.Attachments.EditImage";
  public static final String ATTACH_TEXT = "Jira.Attachments.AttachText";
  public static final String DELETE_ATTACHMENT = "Jira.Attachments.Delete";
  public static final String RENAME_ATTACHMENT = "Jira.Attachments.Rename";
  public static final String VIEW_TIME_REPORT = "Jira.TimeTracking.ViewReport";

  public static final DelegateToLocalAction ASSIGN_TO_ME = new DelegateToLocalAction("Jira.Edit.AssignToMe");

  private static AnAction newIssueAction() {
    return NewItemAction.primary(EditIssueFeature.I18N.getFactory("edit.screens.action.newIssue.name"), Icons.ACTION_CREATE_NEW_ITEM,
      EditIssueFeature.I18N.getFactory("edit.screens.action.newIssue.description"), EditIssueFeature.CREATE_GENERIC);
  }

  private static AnAction newIssueHereAction() {
    return NewItemAction.primary(EditIssueFeature.I18N.getFactory("edit.screens.action.newIssueHere.name"), Icons.ACTION_CREATE_NEW_ITEM,
      EditIssueFeature.I18N.getFactory("edit.screens.action.newIssueHere.description"), EditIssueFeature.CREATE_HERE);
  }

  private static AnAction newSubtask() {
    return NewItemAction.primary(EditIssueFeature.I18N.getFactory("edit.screens.action.createSubtask.name"), Icons.CREATE_SUBTASK,
      EditIssueFeature.I18N.getFactory("edit.screens.action.createSubtask.description"), EditIssueFeature.CREATE_SUBTASK);
  }

  private static EditItemAction editIssueAction() {
    return new EditItemAction(EditIssueFeature.I18N.getFactory("edit.screens.action.edit.name.single"), Icons.ACTION_EDIT_ARTIFACT,
      EditIssueFeature.I18N.getFactory("edit.screens.action.edit.description.single"), EditIssueFeature.EDIT);
  }

  private static AnAction moveIssue() {
    return new EditItemAction(EditIssueFeature.I18N.getFactory("edit.screens.action.move.action.move.name"), Icons.ACTION_MOVE,
      EditIssueFeature.I18N.getFactory("edit.screens.action.move.action.move.description"), MoveIssueFeature.INSTANCE);
  }

  private static AnAction addComment() {
    return NewItemAction.slaves("Add Comment\u2026", Icons.ACTION_COMMENT_ADD,
      L.tooltip("Add a comment to the selected issue"), BaseEditComment.ADD_COMMENT_FEATURE);
  }

  private static EditItemAction editComment() {
    return new EditItemAction("Edit Comment\u2026", Icons.ACTION_COMMENT_EDIT, L.tooltip("Edit selected comment"),
      BaseEditComment.EDIT_COMMENT);
  }

  private static EditItemAction renameAttachment() {
    return new EditItemAction("Rename Attachment", null, null,
      AttachmentNameTopEditor.INSTANCE);
  }

  private static AnAction replyToComment() {
    return NewItemAction.slaves("Reply to Comment\u2026", Icons.ACTION_COMMENT_REPLY,
      L.tooltip("Reply to the selected comment"), BaseEditComment.REPLY_TO_COMMENT);
  }

  private static AnAction addLinks() {
    return NewItemAction.slaves("Create Links\u2026", Icons.ACTION_CREATE_LINK, "Create a new link from this issue",
      AddLinksFeature.INSTANCE);
  }

  public static EditDescriptor.Impl addLinksDescriptor(String title) {
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.notModal("jira.createLinks.outbound.", title, null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setDescriptionStrings(
      "Link Issues",
      "Added links were saved in the local database.",
      "Save new links in the local database without uploading to server",
      "Save new links and upload them to server");
    return descriptor;
  }

  private static EditItemAction editWatches() {
    return new EditItemAction("Edit Watchers\u2026", null, "Edit Watchers for the selected " + Terms.ref_Artifact,
      EditWatchersFeature.INSTANCE);
  }

  private static AnAction createWorklog() {
    return NewItemAction.slaves("Log Work\u2026", Icons.ACTION_WORKLOG_ADD,
      L.tooltip("Add entry to selected issue's work log"), CreateWorklogFeature.INSTANCE);
  }

  private static AnAction deleteWorklog() {
    return new EditItemAction("Delete Work Log", Icons.ACTION_WORKLOG_DELETE, "Delete selected work log entry",
      DeleteWorklogFeature.INSTANCE);
  }

  private static AnAction editWorklog() {
    return new EditItemAction("Edit Work Log", Icons.ACTION_WORKLOG_EDIT, "Edit selected work log entry",
      EditWorklogsFeature.INSTANCE);
  }

  private static AnAction mergeIssue() {
    return new EditItemAction(EditIssueFeature.I18N.getFactory("edit.screens.action.merge.name"), Icons.MERGE_ACTION,
      EditIssueFeature.I18N.getFactory("edit.screens.action.merge.description"), EditIssueFeature.MERGE);
  }

  private static AnAction assignTo() {
    return new EditItemAction("Assign\u2026", Icons.ACTION_ASSIGN, "Assign issue", AssignTo.INSTANCE);
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Edit.NEW_ITEM, newIssueAction());
    registry.registerAction(MainMenu.Edit.EDIT_ITEM, editIssueAction());
    registry.registerAction(MainMenu.Edit.NEW_ITEM_HERE, newIssueHereAction());
    registry.registerAction(MainMenu.Edit.CONFIGURE_FIELDS, ScreenIssueEditor.CONFIGURE_VISIBLE_FIELDS);
    registry.registerAction(MainMenu.Edit.CUSTOM_COPY, new CustomCopyAction());
    registry.registerAction(CREATE_SUBTASK, newSubtask());
    registry.registerAction(ADD_COMMENT, addComment());
    registry.registerAction(EDIT_COMMENT, editComment());
    registry.registerAction(RENAME_ATTACHMENT, renameAttachment());
    registry.registerAction(REPLY_TO_COMMENT, replyToComment());
    registry.registerAction(DELETE_COMMENT, DeleteCommentAction.INSTANCE);
    registry.registerAction(VOTE_FOR_ISSUE, ToggleUserPropertyAction.VOTE);
    registry.registerAction(WATCH_ISSUE, ToggleUserPropertyAction.WATCH);
    registry.registerAction(EDIT_WATCHERS, editWatches());
    registry.registerAction(REMOVE_LINKS, AddLinksFeature.ACTION_REMOVE_LINKS);
    registry.registerAction(ADD_LINKS, addLinks());
    registry.registerAction(LINK_TWO_ISSUES, AddLinksFeature.ACTION_LINK_TWO_ISSUES);
    registry.registerAction(VIEW_ALL_LINKED_ISSUES, AddLinksFeature.ACTION_VIEW_ALL_LINKED);
    registry.registerAction(VIEW_LINKED_ISSUES, AddLinksFeature.ACTION_VIEW_LINKED);
    registry.registerAction(MOVE_ISSUE, moveIssue());
    registry.registerAction(LOG_WORK, createWorklog());
    registry.registerAction(LOG_WORK_DELETE, deleteWorklog());
    registry.registerAction(LOG_WORK_EDIT, editWorklog());
    registry.registerAction(LOG_WORK_ROLLBACK, DiscardWorklogAction.INSTANCE);
    registry.registerAction(MainMenu.Edit.MERGE, mergeIssue());
    registry.registerAction(ASSIGN_TO, assignTo());
    ASSIGN_TO_ME.register(registry);
    if (Env.getBoolean(GlobalProperties.INTERNAL_ACTIONS)) registry.registerAction(MainMenu.Tools.EXPLORER_FIELDS, EditCustomFieldTypesAction.INSTANCE);
    registry.registerAction(MainMenu.Tools.REORDER_TABLE, new JiraReorderAction());
    registry.registerAction(ATTACH_FILE, JiraAttachments.ATTACH_FILE);
    registry.registerAction(ATTACH_SCREEN_SHOT, JiraAttachments.ATTACH_SCREEN_SHOT);
    registry.registerAction(MainMenu.Edit.DOWNLOAD_ATTACHMENTS, JiraAttachments.DOWNLOAD_ATTACHMENTS);
//    registry.registerAction(EDIT_IMAGE_ATTACHMENT, JiraAttachments.EDIT_IMAGE_ATTACHMENT);
    registry.registerAction(ATTACH_TEXT, JiraAttachments.ATTACH_TEXT);
    registry.registerAction(DELETE_ATTACHMENT, JiraAttachments.DELETE_ATTACHMENTS);
    registry.registerAction(EDITOR_ADD_LINKS, LinksEditor.ADD_LINK);
    registry.registerAction(EDITOR_REMOVE_LINKS, LinksEditor.REMOVE_LINKS);
    registry.registerAction(EDITOR_ATTACH_FILES, AttachmentsEditor.EDITOR_ATTACH_FILES);
    registry.registerAction(EDITOR_ATTACH_SCREENSHOT, AttachmentsEditor.EDITOR_ATTACH_SCREENSHOT);
    registry.registerAction(EDITOR_ATTACH_TEXT, AttachmentsEditor.EDITOR_ATTACH_TEXT);
    registry.registerAction(EDITOR_DELETE_ATTACHMENT, AttachmentsEditor.EDITOR_DELETE_ATTACHMENT);
    registry.registerAction(EDITOR_RENAME_ATTACHMENT, AttachmentsEditor.EDITOR_RENAME_ATTACHMENT);
    registry.registerAction(MainMenu.File.RESET_LOGIN_FAILURE, new ResetLoginFailure());
    registry.registerAction(VIEW_TIME_REPORT, new ViewTimeReportAction());
  }

  public static void registerConsoleActions(ConsoleActionsComponent console) {
    // Generic actions
    console.addGroup(new ActionGroup.InContext("", GenericNode.NAVIGATION_NODE, MainMenu.Edit.NEW_ITEM, MainMenu.Edit.NEW_ITEM_HERE));
    // Issue actions
    console.addGroup(new ActionGroup.InContext("Issue", ItemWrapper.ITEM_WRAPPER,
      MainMenu.Edit.EDIT_ITEM, CREATE_SUBTASK, ADD_COMMENT, VOTE_FOR_ISSUE, WATCH_ISSUE, EDIT_WATCHERS, ADD_LINKS, VIEW_ALL_LINKED_ISSUES, MOVE_ISSUE, LOG_WORK,
      MainMenu.Edit.MERGE, ASSIGN_TO, MainMenu.Tools.REORDER_TABLE, ATTACH_FILE, ATTACH_SCREEN_SHOT, ATTACH_TEXT, MainMenu.Edit.DOWNLOAD_ATTACHMENTS));
    // Issue actions (editor)
    console.addGroup(new ActionGroup.Simple("", EDITOR_ADD_LINKS, EDITOR_ATTACH_FILES, EDITOR_ATTACH_SCREENSHOT, EDITOR_ATTACH_TEXT, EDITOR_REMOVE_LINKS, EDITOR_DELETE_ATTACHMENT, EDITOR_RENAME_ATTACHMENT));
    // Sub issue actions
    console.addGroup(new ActionGroup.InContext("Comments", CommentImpl.DATA_ROLE, EDIT_COMMENT, REPLY_TO_COMMENT, DELETE_COMMENT));
    console.addGroup(new ActionGroup.InContext("Attachments", AttachmentImpl.ROLE, RENAME_ATTACHMENT, DELETE_ATTACHMENT));
    console.addGroup(new ActionGroup.InContext("Links", LoadedLink2.DB_LINK, REMOVE_LINKS, VIEW_LINKED_ISSUES));
    console.addGroup(new ActionGroup.InContext("Work Log", LoadedWorklog.WORKLOG, LOG_WORK_EDIT, LOG_WORK_DELETE, LOG_WORK_ROLLBACK));
  }

  public static String prependIssueKey(List<ItemWrapper> issues, String suffix) {
    String title = suffix;
    if (issues.size() == 1) {
      String issueKey = LoadedIssueUtil.getIssueKey(issues.get(0));
      if (issueKey != null && issueKey.length() > 0) title = issueKey + " - " + title;
    }
    return title;
  }

  public static String prependIssueKey(ItemWrapper issue, String suffix) {
    if (issue == null) return suffix;
    else return prependIssueKey(Collections.singletonList(issue), suffix);
  }
}
