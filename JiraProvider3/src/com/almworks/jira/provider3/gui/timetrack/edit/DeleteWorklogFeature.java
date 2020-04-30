package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersionCreator;
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
import java.util.List;

public class DeleteWorklogFeature extends TopEditor {
  private static final TypedKey<LongList> WORKLOG_ITEMS = TypedKey.create("worklogItems");
  public static final EditFeature INSTANCE = new DeleteWorklogFeature();

  private DeleteWorklogFeature() {
    super(NameMnemonic.rawText("Delete Work Log"));
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    updateRequest.watchRole(LoadedWorklog.WORKLOG);
    List<LoadedWorklog> worklogs = context.getSourceCollection(LoadedWorklog.WORKLOG);
    CantPerformException.ensureNotEmpty(worklogs);
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    String title = JiraActions.prependIssueKey(issue, "Delete Work Log");
    EditDescriptor.Impl result = EditDescriptor.Impl.frame("worklog.", title, null);
    result.setDescriptionStrings(
      "Delete Work Log",
      "Updated issue was saved in the local database.",
      "Save updated issue in the local database without uploading to server",
      "Save updated issue and upload it to server");
    result.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    if (IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_DELETE_ALL)) return result;
    if (TimeUtils.isAllLocal(worklogs)) return result;
    if (!IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_DELETE_OWN)) throw new CantPerformException();
    TimeUtils.isAllOwn(issue, worklogs);
    return result;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock)
    throws CantPerformException
  {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    List<LoadedWorklog> worklogs = CantPerformException.ensureNotEmpty(context.getSourceCollection(LoadedWorklog.WORKLOG));
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(issue.getItem()));
    LongArray worklogItems = LongArray.create(UiItem.GET_ITEM.collectList(worklogs));
    model.putHint(WORKLOG_ITEMS, worklogItems);
    itemsToLock.addAll(worklogItems);
    EngineConsts.setupConnection(model, JiraEditUtils.getIssueConnection(issue));
    return model;
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> pair = createDefaultNestedModel(parent, WorklogForm.ADJUSTMENT);
    pair.getFirst().copyHint(parent, WORKLOG_ITEMS);
    return pair;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return true;
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel model, Configuration config) {
    WorklogForm form = new WorklogForm(config);
    AdjustmentEditor.INSTANCE.attach(life, model, form);
    JComponent component = form.getComponent();
    FieldEditorUtil.setupTopWhitePanel(life, component);
    return component;
  }

  @Override
  protected void doCommit(CommitContext childContext) {
    LongList worklogs = childContext.getModel().getValue(WORKLOG_ITEMS);
    AdjustmentEditor.INSTANCE.commitAdjust(childContext, worklogs);
    for (ItemVersionCreator worklog : childContext.getDrain().changeItems(worklogs)) worklog.delete();
  }
}
