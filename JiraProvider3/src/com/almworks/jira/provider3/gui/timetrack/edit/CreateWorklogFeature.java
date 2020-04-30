package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.InplaceNewSlave;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.timetrack.TimeUtils;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class CreateWorklogFeature extends TopEditor {
  private static final ItemCreator WORKLOG_CREATOR = new ItemCreator() {
    @Override
    public void setupNewItem(EditModelState model, ItemVersionCreator item) {
      long connection = EngineConsts.getConnectionItem(model);
      if (connection <= 0) {
        LogHelper.error("Missing connection");
        throw new DBOperationCancelledException();
      }
      Long thisUser = EditMetaSchema.getThisUser(item.getReader(), model);
      if (thisUser == null || thisUser <= 0) throw new DBOperationCancelledException();
      initNewWorklog(item, connection, thisUser);
    }
  };

  private static final InplaceNewSlave WORKLOG_SLAVE = InplaceNewSlave.addNewWhenEditMaster(NameMnemonic.rawText("Log Work"), WORKLOG_CREATOR,
    Worklog.ISSUE, WorklogForm.START, WorklogForm.COMMENT, WorklogForm.TIME_SPENT, WorklogForm.VISIBILITY);

  public static final EditFeature INSTANCE = new CreateWorklogFeature();

  private CreateWorklogFeature() {
    super(NameMnemonic.rawText("Create Work Log"));
  }

  private static void initNewWorklog(ItemVersionCreator item, long connection, long thisUser) {
    item.setValue(DBAttribute.TYPE, Worklog.DB_TYPE);
    item.setValue(SyncAttributes.CONNECTION, connection);
    item.setValue(Worklog.AUTHOR, thisUser);
  }

  @Nullable
  public static ItemVersionCreator createNewWorklog(ItemVersionCreator issue) {
    ItemVersion connection = issue.readValue(SyncAttributes.CONNECTION);
    if (connection == null) {
      LogHelper.error("Issue missing connection", issue);
      return null;
    }
    long user = connection.getNNValue(Connection.USER, 0l);
    ItemVersionCreator item = issue.createItem();
    initNewWorklog(item, connection.getItem(), user);
    item.setValue(Worklog.ISSUE, issue);
    return item;
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    CantPerformException.ensure(TimeUtils.canCreateWorklog(issue));
    String title = JiraActions.prependIssueKey(issue, "Log Work");
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("worklog.", title, null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setContextKey(JiraEditUtils.getContextKey(this, context));
    descriptor.setDescriptionStrings(
      "Log Work",
      "New work log was saved in the local database.",
      "Save work log in the local database without uploading to server",
      "Save work log and upload it to server");
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock)
    throws CantPerformException
  {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(issue.getItem()));
    EngineConsts.setupConnection(model, JiraEditUtils.getIssueConnection(issue));
    return model;
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    return createDefaultNestedModel(parent, WORKLOG_SLAVE, WorklogForm.ADJUSTMENT);
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel issue, Configuration config) {
    DefaultEditModel.Child worklog = WORKLOG_SLAVE.getNestedModel(issue);
    if (issue == null || worklog == null) return null;
    WorklogForm form = new WorklogForm(config);
    form.attachComment(life, worklog, WorklogForm.COMMENT, config);
    form.attachStart(life, worklog, WorklogForm.START);
    form.attachTimeSpent(life, worklog, WorklogForm.TIME_SPENT, config);
    form.attachVisibility(life, worklog, WorklogForm.VISIBILITY, config);
    WorklogForm.ADJUSTMENT.attach(life, issue, form);
    JComponent component = form.getComponent();
    FieldEditorUtil.setupTopWhitePanel(life, component);
    return component;
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    ItemVersionCreator newWorklog = WORKLOG_SLAVE.commitNew(childContext);
    if (newWorklog == null) return;
    WorklogForm.ADJUSTMENT.commitAdjust(childContext, LongArray.create(newWorklog.getItem()));
  }
}
