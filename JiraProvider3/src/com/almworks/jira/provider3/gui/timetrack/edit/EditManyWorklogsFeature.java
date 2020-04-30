package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.jira.provider3.gui.timetrack.TimeUtils;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;

import java.util.List;

class EditManyWorklogsFeature extends VerticalLinePlacement.EditImpl {
  public static final EditFeature INSTANCE = new EditManyWorklogsFeature();
  public static final List<BaseFieldEditor> EDITORS = Collections15.unmodifiableListCopy(WorklogForm.COMMENT, WorklogForm.VISIBILITY);

  @Override
  protected List<? extends FieldEditor> getEditors(EditItemModel model) {
    return EDITORS;
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(LoadedWorklog.WORKLOG);
    List<LoadedWorklog> worklogs = context.getSourceCollection(LoadedWorklog.WORKLOG);
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    String title = JiraActions.prependIssueKey(issue, "Edit Work Logs");
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("worklogs.", title, null);
    descriptor.putPresentationProperty(PresentationKey.NAME, "Edit Work Logs");
    descriptor.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Edit comments and visibilities of the selected work logs");
    descriptor.setDescriptionStrings(
      "Edit Work Logs",
      "Updated work logs was saved in the local database.",
      "Save updated work logs in the local database without uploading to server",
      "Save updated work logs and upload them to server");
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    if (IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_EDIT_ALL)) return descriptor;
    if (TimeUtils.isAllLocal(worklogs)) return descriptor;
    if (!IssuePermissions.hasPermission(issue, IssuePermissions.WORKLOG_EDIT_OWN)) throw new CantPerformException();
    TimeUtils.isAllOwn(issue, worklogs);
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    List<LoadedWorklog> worklogs = context.getSourceCollection(LoadedWorklog.WORKLOG);
    LongArray worklogItems = ItemActionUtils.collectItems(worklogs);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(worklogItems);
    itemsToLock.addAll(worklogItems);
    EngineConsts.setupConnection(model, JiraEditUtils.getIssueConnection(issue));
    return model;
  }
}
