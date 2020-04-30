package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.ui.actions.*;

import java.util.Collections;
import java.util.List;

class WorkflowEditFeature extends VerticalLinePlacement.EditImpl {
  private static final WFActionEditor2 EDITOR = new WFActionEditor2.Builder().addBottom(ServerFields.LINKS).create();

  private final ActionApplication myApplication;
  private final List<ItemWrapper> myIssues;

  public WorkflowEditFeature(ActionApplication application, List<ItemWrapper> issues) {
    myApplication = application;
    myIssues = issues;
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    CantPerformException.ensure(myApplication.getConnection() == pair.getFirst().getConnectionItem());
    List<ItemWrapper> primaryItems = pair.getSecond();
    return myApplication.createEditDescriptor(primaryItems);
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3, List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    List<ItemWrapper> wrappers = pair.getSecond();
    LongArray items = LongArray.create(UiItem.GET_ITEM.collectList(wrappers));
    JiraConnection3 connection = pair.getFirst();
    if (items.isEmpty()) throw new CantPerformExceptionSilently("Nothing to edit");
    itemsToLock.addAll(items);
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(items);
    EngineConsts.setupConnection(model, connection);
    EDITOR.setActionApplication(model, myApplication);
    return model;
  }

  @Override
  protected List<? extends FieldEditor> getEditors(EditItemModel model) {
    return Collections.singletonList(EDITOR);
  }

  public void listenIssues(UpdateContext context) throws CantPerformException {
    context.watchModifiableRole(SyncManager.MODIFIABLE);
    ItemActionUtils.checkNotLocked(context, myIssues);
  }
}
