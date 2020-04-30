package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.multi.EnumSubsetDiffEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.remotedata.issue.fields.JsonUserParser;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.User;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.Local;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public class EditWatchersFeature implements EditFeature {
  public static final EditFeature INSTANCE = new EditWatchersFeature();

  private final EnumSubsetDiffEditor myEditor = new EnumSubsetDiffEditor(NameMnemonic.rawText("Watchers"), Issue.WATCHERS, ConnectionVariants.createStatic(
    User.ENUM_TYPE, MetaSchema.CONFIG_WATCHERS), User.CREATOR, null) {
    @Override
    public void commit(CommitContext context) throws CancelCommitException {
      super.commit(context);
      ItemVersionCreator issue = context.getCreator();
      Set<Long> items = Util.NN(issue.getValue(Issue.WATCHERS), Collections15.<Long>emptySet());
      issue.setValue(Issue.WATCHERS_COUNT, items.size());
      Long me = EditMetaSchema.getThisUser(context.getReader(), context.getModel());
      issue.setValue(Issue.WATCHING, items.contains(Util.NN(me, -1L)));
    }

    @Override
    public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
      super.prepareModel(source, model, editPrepare);
      JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, model);
      if (connection != null) {
        JsonUserParser.LoadedUser thisUser = connection.getConfigHolder().getConnectionLoadedUser();
        if (thisUser != null) {
          if (thisUser.getAccountId() != null)
            disableNewItems(model); // This connection identifies uses by AccountID. We cannot expect that JC user will provide AccountID for a new user, so disable "New user" feature.
        } else LogHelper.error("Missing this user", connection);
      } else LogHelper.error("Missing connection");
    }
  };

  private EditWatchersFeature() {
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    List<ItemWrapper> issues = pair.getSecond();
    JiraConnection3 connection = pair.getFirst();
    CantPerformException.ensure(connection.isUploadAllowed());
    CantPerformException.ensure(ItemDownloadStageKey.isAllHasActualDetails(issues));
    for (ItemWrapper issue : issues)
      CantPerformException.ensure(IssuePermissions.hasPermission(issue, IssuePermissions.MANAGER_WATCHERS));
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("editWatchers.", JiraActions.prependIssueKey(issues, "Edit Watchers"), null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setDescriptionStrings(
      "Edit Watchers",
      Local.parse("Updated " + Terms.ref_artifact + " was saved in the local database."),
      "Save changes in the local database without uploading to server",
      "Save changes and upload to server");
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    JiraConnection3 connection = pair.getFirst();
    List<ItemWrapper> issues = pair.getSecond();
    List<Long> itemList = ItemWrapper.GET_ITEM.collectList(issues);
    LongArray items = LongArray.create(itemList);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(items);
    itemsToLock.addAll(items);
    EngineConsts.setupConnection(model, connection);
    return model;
  }

  @Override
  public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
    myEditor.prepareModel(BranchSource.trunk(reader), model, editPrepare);
  }

  @NotNull
  @Override
  public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
    List<? extends ComponentControl> components = myEditor.createComponents(life, model);
    if (components.size() != 1) {
      LogHelper.error(components, editorConfig);
      return new JLabel("Error: cannot edit watchers");
    }
    JComponent component = components.get(0).getComponent();
    FieldEditorUtil.setupTopWhitePanel(life, component);
    return component;
  }
}
