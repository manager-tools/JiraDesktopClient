package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.jira.provider3.gui.timetrack.TimeUtils;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

class EditSingleWorklogFeature extends TopEditor {
  private static final TypedKey<Long> WORKLOG = TypedKey.create("worklog");
  private static final WorklogEditor WORKLOG_SLAVE = new WorklogEditor();

  public static final EditFeature INSTANCE = new EditSingleWorklogFeature();

  private EditSingleWorklogFeature() {
    super(NameMnemonic.rawText("Edit Work Log"));
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(LoadedWorklog.WORKLOG);
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    LoadedWorklog worklog = context.getSourceObject(LoadedWorklog.WORKLOG);
    JiraEditUtils.getIssueConnection(context.getSourceObject(ItemWrapper.ITEM_WRAPPER));
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    String title = JiraActions.prependIssueKey(issue, "Edit Work Log");
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("worklog.", title, null);
    descriptor.setDescriptionStrings(
      "Edit Work Log",
      "Updated work log was saved in the local database.",
      "Save updated work log in the local database without uploading to server",
      "Save updated work log and upload it to server");
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    if (IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_EDIT_ALL)) return descriptor;
    if (TimeUtils.isAllLocal(Arrays.asList(worklog))) return descriptor;
    if (!IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_EDIT_OWN)) throw new CantPerformException();
    TimeUtils.isAllOwn(issue, Arrays.asList(worklog));
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock)
    throws CantPerformException
  {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    LoadedWorklog worklog = context.getSourceObject(LoadedWorklog.WORKLOG);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(issue.getItem()));
    model.putHint(WORKLOG, worklog.getItem());
    itemsToLock.addAll(worklog.getItem());
    EngineConsts.setupConnection(model, JiraEditUtils.getIssueConnection(issue));
    return model;
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> pair = createDefaultNestedModel(parent, WORKLOG_SLAVE, WorklogForm.ADJUSTMENT);
    pair.getFirst().copyHint(parent, WORKLOG);
    return pair;
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel issue, Configuration config) {
    DefaultEditModel.Child worklog = WORKLOG_SLAVE.getNestedModel(issue);
    if (issue == null || worklog == null) return null;
    WorklogForm form = new WorklogForm(config);
    form.attachComment(life, worklog, WorklogForm.COMMENT, null);
    form.attachStart(life, worklog, WorklogForm.START);
    form.attachTimeSpent(life, worklog, WorklogForm.TIME_SPENT, null);
    form.attachVisibility(life, worklog, WorklogForm.VISIBILITY, null);
    WorklogForm.ADJUSTMENT.attach(life, issue, form);
    JComponent component = form.getComponent();
    FieldEditorUtil.setupTopWhitePanel(life, component);
    return component;
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    WORKLOG_SLAVE.commit(childContext);
    Long worklog = childContext.getModel().getValue(WORKLOG);
    WorklogForm.ADJUSTMENT.commitAdjust(childContext, worklog != null ? LongArray.create(worklog) : LongList.EMPTY);
  }

  private static class WorklogEditor extends NestedModelEditor {
    protected WorklogEditor() {
      super(NameMnemonic.rawText("Work Log"));
    }

    @Override
    protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare)
    {
      Long worklog = parent.getValue(WORKLOG);
      return Pair.create(DefaultEditModel.Child.editItem(parent, worklog, false),
        Arrays.asList(WorklogForm.START, WorklogForm.COMMENT, WorklogForm.TIME_SPENT, WorklogForm.VISIBILITY));
    }

    @Override
    public void commit(CommitContext context) throws CancelCommitException {
      Long worklogItem = context.getModel().getValue(WORKLOG);
      if (worklogItem == null || worklogItem < 0) return;
      commitNested(context.itemContext(worklogItem));
    }
  }
}
