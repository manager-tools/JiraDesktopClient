package com.almworks.jira.provider3.issue.features.edit;

import com.almworks.api.application.DBDataRoles;
import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.ItemCreator;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.DefaultValues;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.gui.edit.editors.move.MoveController;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.issue.features.edit.screens.ScreenChooser;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.jetbrains.annotations.NotNull;

class CreateIssueFeature extends BaseEditIssueFeature {
  private static final String MULTIPLE_NEW_ISSUE = "newIssue.allowMultiple";

  static final ItemCreator ISSUE_CREATOR = new ItemCreator() {
    @Override
    public void setupNewItem(EditModelState model, ItemVersionCreator item) {
      long connection = EngineConsts.getConnectionItem(model);
      if (connection <= 0) {
        LogHelper.error("Missing connection");
        throw new DBOperationCancelledException();
      }
      item.setValue(SyncAttributes.CONNECTION, connection);
      item.setValue(DBAttribute.TYPE, Issue.DB_TYPE);
      item.setValue(SyncAttributes.ITEM_DOWNLOAD_STAGE, ItemDownloadStage.NEW.getDbValue());
    }
  };
  private static final Condition<DBAttribute<?>> SUBTASK_DEFAULTS = Condition.inCollection(Issue.ISSUE_TYPE);
  static final Condition<DBAttribute<?>> GENERIC_DEFAULTS = Condition.inCollection(Issue.PROJECT, Issue.ISSUE_TYPE);
  static final ScreenIssueEditor EDITOR = new ScreenIssueEditor(false, new ScreenChooser(BaseEditIssueFeature.SCHEME, true));

  private final boolean mySubtask;

  public CreateIssueFeature(boolean subtask) {
    super(EDITOR);
    mySubtask = subtask;
  }

  @NotNull
  @Override
  protected EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    if (mySubtask) {
      updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
      ItemWrapper parent = getContextIssue(context);
      JiraConnection3 connection = JiraEditUtils.getIssueConnection(parent);
      CantPerformException.ensure(connection.isUploadAllowed());
      ItemKey type = parent.getModelKeyValue(MetaSchema.issueType(context));
      CantPerformException.ensure(IssueType.isSubtask(type, false));
      checkAnySubTaskTypesInProject(parent.getModelKeyValue(MetaSchema.project(context)), connection);
      EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("new.", JiraActions.prependIssueKey(parent, EditIssueFeature.I18N.getString("edit.screens.window.createSubtask.title")), EditIssueFeature.getWindowPrefSize());
      if (!isMultipleAllowed()) descriptor.setContextKey(Pair.create(this, parent.getItem()));
      descriptor.setDescriptionStrings(
        EditIssueFeature.I18N.getString("edit.screens.window.createSubtask.notUploaded.title"),
        EditIssueFeature.I18N.getString("edit.screens.window.createSubtask.notUploaded.message"),
        EditIssueFeature.I18N.getString("edit.screens.window.createSubtask.saveDescription"),
        EditIssueFeature.I18N.getString("edit.screens.window.createSubtask.uploadDescription"));
      commonDescriptorSetup(descriptor);
      return descriptor;
    } else {
      updateRequest.watchRole(GenericNode.NAVIGATION_NODE);
      JiraConnection3 connection = findContextConnection(context);
      CantPerformException.ensure(connection.isUploadAllowed());
      return newIssueDescriptor(connection);
    }
  }

  private static JiraConnection3 findContextConnection(ActionContext context) throws CantPerformException {
    return CantPerformException.cast(JiraConnection3.class, DBDataRoles.findConnection(context));
  }

  private static ItemWrapper getContextIssue(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    CantPerformException.ensure(!issue.services().isRemoteDeleted());
    return issue;
  }

  private static boolean isMultipleAllowed() {
    return Env.getBoolean(MULTIPLE_NEW_ISSUE, false);
  }

  private static void checkAnySubTaskTypesInProject(ItemKey project, JiraConnection3 connection) throws CantPerformException {
    long projectItem = project == null ? 0L : project.getItem();
    if (projectItem > 0L) {
      ItemHypercubeImpl forTypes = new ItemHypercubeImpl();
      forTypes.addAxisIncluded(Issue.PROJECT, new LongList.Single(projectItem));
      forTypes.addAxisIncluded(SyncAttributes.CONNECTION, new LongList.Single(connection.getConnectionItem()));
      CantPerformException.ensure(MoveController.IS_SUBTASK.hasAny(
        connection.getGuiFeatures().getEnumTypes()
          .getType(IssueType.ENUM_TYPE)
          .getEnumValues(forTypes)));
    }
  }

  static EditDescriptor.Impl newIssueDescriptor(JiraConnection3 connection) {
    EditDescriptor.Impl descriptor;
    descriptor = EditDescriptor.Impl.frame("new.", EditIssueFeature.I18N.getString("edit.screens.window.newIssue.title"), EditIssueFeature.getWindowPrefSize());
    descriptor.setActionScope("NewArtifact");
    if (!isMultipleAllowed()) descriptor.setContextKey(connection);
    descriptor.setDescriptionStrings(
      EditIssueFeature.I18N.getString("edit.screens.window.newIssue.notUploaded.title"),
      EditIssueFeature.I18N.getString("edit.screens.window.newIssue.notUploaded.message"),
      EditIssueFeature.I18N.getString("edit.screens.window.newIssue.saveDescription"),
      EditIssueFeature.I18N.getString("edit.screens.window.newIssue.uploadDescription"));
    commonDescriptorSetup(descriptor);
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    JiraConnection3 connection;
    DefaultEditModel.Root model = DefaultEditModel.Root.newItem(ISSUE_CREATOR);
    DefaultValues defaults;
    if (mySubtask) {
      ItemWrapper issue = getContextIssue(context);
      MoveController.setNewSubtaskParent(model, issue.getItem());
      connection = JiraEditUtils.getIssueConnection(issue);
      defaults = new DefaultValues(new ItemReference.GetItem(new ItemProxy.Item(issue.getItem()), Issue.PROJECT), Project.SUBTASK_DEFAULTS, SUBTASK_DEFAULTS);
    } else {
      connection = findContextConnection(context);
      defaults = new DefaultValues(connection.getConnectionObj(), Issue.ISSUE_DEFAULTS, GENERIC_DEFAULTS);
    }
    EngineConsts.setupConnection(model, connection);
    setDefaults(model, defaults);
    return model;
  }
}
