package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.UiItem;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.comments.gui.BaseEditComment;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.List;
import java.util.Map;

class AssignTo extends TopEditor {
  public static final EditFeature INSTANCE = new AssignTo();

  private AssignTo() {
    super(NameMnemonic.rawText("Assign To"));
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    updateRequest.watchRole(LoadedItem.ITEM_WRAPPER);
    updateRequest.watchModifiableRole(SyncManager.MODIFIABLE);
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    CantPerformException.ensure(pair.getFirst().isUploadAllowed());
    String title = JiraActions.prependIssueKey(pair.getSecond(), "Assign To");
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.notModal("jira.assignTo.", title, null);
    JiraEditUtils.checkAnyConnectionAllowsEdit(context, descriptor);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setDescriptionStrings(
      "Assign To",
      "Updated issue was saved in the local database.",
      "Save updated issues in the local database without uploading to server",
      "Save updated issues and upload to server");
    ItemActionUtils.checkNotLocked(context, pair.getSecond());
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    LongArray issues = LongArray.create(UiItem.GET_ITEM.collectList(pair.getSecond()));
    CantPerformException.ensure(!issues.isEmpty());
    itemsToLock.addAll(issues);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(issues);
    EngineConsts.setupConnection(model, pair.getFirst());
    return model;
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    return createDefaultNestedModel(parent, EditMetaSchema.PROVIDE_PROJECT, EditMetaSchema.ASSIGNEE, BaseEditComment.COMMENT_SLAVE);
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel model, Configuration config) {
    List<ComponentControl> components = Collections15.arrayList();
    Map<FieldEditor, List<ComponentControl>> byEditor = Collections15.hashMap();
    FieldEditorUtil.createComponents(life, model, model.getAllEditors(), components, byEditor);
    List<ComponentControl> assignee = byEditor.get(EditMetaSchema.ASSIGNEE);
    if (assignee != null)
      for (ComponentControl control : assignee) {
        int index = components.indexOf(control);
        if (index < 0) LogHelper.error("Not found", control, components);
        else {
          ComponentControl.Enabled enabled = control.getEnabled();
          if (enabled == ComponentControl.Enabled.ENABLED || enabled == ComponentControl.Enabled.DISABLED)
            components.set(index, ComponentControl.EnableWrapper.wrap(true, control));
        }
      }
    else LogHelper.error("Assignee editors not found");
    return VerticalLinePlacement.buildComponent(life, model, components);
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    childContext.commitEditors(null);
  }
}
