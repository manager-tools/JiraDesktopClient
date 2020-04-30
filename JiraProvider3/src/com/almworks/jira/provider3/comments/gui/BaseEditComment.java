package com.almworks.jira.provider3.comments.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.ItemCreator;
import com.almworks.items.gui.edit.editors.ConstEditor;
import com.almworks.items.gui.edit.editors.composition.InplaceNewSlave;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.edit.editors.VisibilityEditor;
import com.almworks.jira.provider3.gui.viewer.CommentImpl;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.speedsearch.TextSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.LineTokenizer;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static com.almworks.util.ui.actions.ActionUtil.getNullable;

public abstract class BaseEditComment implements EditFeature {
  private static final ItemCreator COMMENT_CREATOR = new ItemCreator() {
    @Override
    public void setupNewItem(EditModelState model, ItemVersionCreator item) {
      setupNewComment(model, item);
    }
  };

  static final ScalarFieldEditor<String> COMMENT_TEXT = ScalarFieldEditor.textPane(NameMnemonic.parseString("Co&mment"), Comment.TEXT);
  static final DropdownEnumEditor COMMENT_VISIBILITY = VisibilityEditor.create(Comment.LEVEL);

  public static final InplaceNewSlave COMMENT_SLAVE = new InplaceNewSlave(NameMnemonic.parseString("Co&mment"), COMMENT_CREATOR, Comment.ISSUE, Arrays.asList(COMMENT_TEXT, COMMENT_VISIBILITY)) {
    @Override
    public boolean hasDataToCommit(EditItemModel model) {
      boolean superHas = super.hasDataToCommit(model);
      if (!superHas) return false;
      DefaultEditModel.Child nested = getNestedModel(model);
      String text = Util.NN(COMMENT_TEXT.getCurrentValue(nested), "").trim();
      return !text.isEmpty();
    }

    @Override
    public boolean isChanged(EditItemModel model) {
      DefaultEditModel.Child nested = getNestedModel(model);
      return nested != null && COMMENT_TEXT.isChanged(nested);
    }
  };

  public static final EditFeature ADD_COMMENT_FEATURE = new BaseEditComment() {
    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
      List<ItemWrapper> issues = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
      if (!JiraEditUtils.selectIssuesWrappers(context).getFirst().isUploadAllowed()) throw new CantPerformException();
      String title = JiraActions.prependIssueKey(issues, "Add Comment");
      EditDescriptor.Impl descriptor = createDescriptor(context, title, "addComment.");
      descriptor.setDescriptionStrings(
        "Add Comment",
        "New comment was saved in the local database.",
        "Save new comment in the local database without uploading to server",
        "Save new comment and upload it to server");
      return descriptor;
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
      Pair<JiraConnection3, List<ItemWrapper>> issues = JiraEditUtils.selectIssuesWrappers(context);
      LongArray issueItems = LongArray.create(UiItem.GET_ITEM.collectList(issues.getSecond()));
      if (issueItems.isEmpty())
        throw new CantPerformException();
      DefaultEditModel.Root model = DefaultEditModel.Root.editItems(issueItems);
      EngineConsts.setupConnection(model, issues.getFirst());
      return model;
    }

    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, EditPrepare editPrepare) {
      COMMENT_SLAVE.prepareModel(BranchSource.trunk(reader), model, editPrepare);
    }

    @Nullable
    @Override
    public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
      DefaultEditModel nested = COMMENT_SLAVE.getNestedModel(model);
      if (nested == null) return null;
      return super.editModel(life, nested, editorConfig);
    }
  };

  public static final BaseEditComment EDIT_COMMENT = new BaseEditComment() {
    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      updateRequest.watchRole(CommentImpl.DATA_ROLE);
      ItemWrapper issue = getNullable(context, ItemWrapper.ITEM_WRAPPER);
      String title = JiraActions.prependIssueKey(issue, "Edit Comment");
      EditDescriptor.Impl descriptor = createDescriptor(context, title, "editComment.");
      checkUploadAllowed(context, descriptor);
      CommentImpl comment = context.getSourceObject(CommentImpl.DATA_ROLE);
      if (context.getSourceObject(SyncManager.ROLE).findLock(comment.getItem()) != null) throw new CantPerformException();
      checkPermissions(comment, issue);
      descriptor.setDescriptionStrings(
        "Edit Comment",
        "Updated comment was saved in the local database.",
        "Save updated comment in the local database without uploading to server",
        "Save updated comment and upload it to server");
      return descriptor;
    }

    private void checkPermissions(CommentImpl comment, @Nullable ItemWrapper issue) throws CantPerformException {
      if (issue == null) return ;
      if (comment.isLocalNew()) return;
      JiraConnection3 connection = CantPerformException.cast(JiraConnection3.class, issue.getConnection());
      boolean ownComment = comment.getAuthor() == connection.getConnectionUser();
      CantPerformException.ensure(IssuePermissions.hasPermission(issue, ownComment ? IssuePermissions.COMMENT_EDIT_OWN : IssuePermissions.COMMENT_EDIT_ALL));
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
      CommentImpl comment = context.getSourceObject(CommentImpl.DATA_ROLE);
      JiraConnection3 connection = getMasterConnection(context);
      long commentItem = comment.getItem();
      DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(commentItem));
      itemsToLock.add(commentItem);
      EngineConsts.setupConnection(model, connection);
      return model;
    }

    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, EditPrepare editPrepare) {
      Form.prepareEdit(BranchSource.trunk(reader), model, editPrepare);
    }
  };

  public static final BaseEditComment REPLY_TO_COMMENT = new BaseEditComment() {
    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
      updateRequest.watchRole(CommentImpl.DATA_ROLE);
      ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
      context.getSourceObject(CommentImpl.DATA_ROLE);
      String title = JiraActions.prependIssueKey(issue, "Reply to Comment");
      EditDescriptor.Impl descriptor = createDescriptor(context, title, "addComment.");
      checkUploadAllowed(context, descriptor);
      JiraEditUtils.checkAnyConnectionAllowsEdit(context, descriptor);
      descriptor.setDescriptionStrings(
        "Reply to Comment",
        "New comment was saved in the local database.",
        "Save new comment in the local database without uploading to server",
        "Save new comment and upload it to server");
      return descriptor;
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock)
      throws CantPerformException
    {
      CommentImpl comment = context.getSourceObject(CommentImpl.DATA_ROLE);
      JiraConnection3 connection = getMasterConnection(context);
      DefaultEditModel.Root model = DefaultEditModel.Root.newItem(COMMENT_CREATOR);
      ItemWrapper wrapper = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
      ConstEditor.install(model, Comment.ISSUE, wrapper.getItem());
      String text = comment.getText();
      text = LineTokenizer.prependLines(text, "> ");
      COMMENT_TEXT.setValue(model, text);
      COMMENT_VISIBILITY.setValueItem(model, comment.getVisibleForItem());
      EngineConsts.setupConnection(model, connection);
      return model;
    }

    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, EditPrepare editPrepare) {
      Form.prepareEdit(BranchSource.trunk(reader), model, editPrepare);
    }
  };
//  public static final String NEXT_FORWARD = "Jira.Merge.Comments.NextForward";
//  public static final String NEXT_BACKWARD = "Jira.Merge.Comments.NextBackward";
//  public static final String RESOLVE_EDIT = "Jira.Merge.Comments.Resolve.Edit";
//  public static final String RESOLVE_DISCARD = "Jira.Merge.Comments.Resolve.Discard";
//  public static final String RESOLVE_NEW = "Jira.Merge.Comments.Resolve.New";

  private static void checkUploadAllowed(ActionContext context, EditDescriptor.Impl descriptor) throws CantPerformException {
    JiraEditUtils.checkAnyConnectionAllowsEdit(context, descriptor);
    CantPerformException.ensure(getMasterConnection(context).isUploadAllowed());
  }

  private static JiraConnection3 getMasterConnection(ActionContext context) throws CantPerformException {
    return CantPerformException.cast(JiraConnection3.class,
      context.getSourceObject(ItemWrapper.ITEM_WRAPPER).getConnection());
  }

  protected BaseEditComment() {}

  protected  EditDescriptor.Impl createDescriptor(ActionContext context, String title, String windowId) throws CantPerformException {
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame(windowId, title, new Dimension(300, 200));
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setContextKey(JiraEditUtils.getContextKey(this, context));
    return descriptor;
  }

  @Nullable
  @Override
  public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
    JComponent component = Form.createComponent(life, model);
    FieldEditorUtil.setupTopWhitePanel(life, component);
    return component;
  }

  public static void attachComponent(Lifespan life, EditItemModel commentModel, JTextPane comment, AComboBox<ItemKey> visibleFor) {
    if (commentModel != null) {
      COMMENT_TEXT.attachComponent(life, commentModel, comment);
      COMMENT_VISIBILITY.attachCombo(life, commentModel, visibleFor);
    }
  }

  private static void setupNewComment(EditModelState model, ItemVersionCreator item) {
    long connection = EngineConsts.getConnectionItem(model);
    if (connection <= 0) {
      LogHelper.error("Missing connection");
      throw new DBOperationCancelledException();
    }
    long thisUser = Util.NN(EditMetaSchema.getThisUser(item.getReader(), model), 0l);
    setupNewComment(item, connection, thisUser);
  }

  private static void setupNewComment(ItemVersionCreator comment, long connection, long thisUser) {
    comment.setValue(DBAttribute.TYPE, Comment.DB_TYPE);
    comment.setValue(SyncAttributes.CONNECTION, connection);
    if (thisUser > 0) comment.setValue(Comment.AUTHOR, thisUser);
  }

  static ItemVersionCreator createNewComment(ItemVersionCreator issue) {
    ItemVersion connection = issue.readValue(SyncAttributes.CONNECTION);
    if (connection == null) {
      LogHelper.error("Issue missing connection", issue);
      return null;
    }
    long user = connection.getNNValue(Connection.USER, 0l);
    ItemVersionCreator comment = issue.createItem();
    comment.setValue(Comment.ISSUE, issue);
    setupNewComment(comment, connection.getItem(), user);
    return comment;
  }

  private static class Form {
    private JPanel myWholePanel;
    private JTextPane myText;
    private AComboBox<ItemKey> myVisibleTo;

    public static JComponent createComponent(Lifespan life, EditItemModel model) {
      Form form = new Form();
      COMMENT_TEXT.attachComponent(life, model, form.myText);
      COMMENT_VISIBILITY.attachCombo(life, model, form.myVisibleTo);
      TextSpeedSearch.installCtrlF(form.myText);
      return form.myWholePanel;
    }

    public static void prepareEdit(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
      COMMENT_TEXT.prepareModel(source, model, editPrepare);
      COMMENT_VISIBILITY.prepareModel(source, model, editPrepare);
    }
  }
}
