package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.app.connection.JiraProvider3;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringConvertingListDecorator;
import com.almworks.util.collections.Convertor;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class ActionNameWrapper implements IdentifiableAction {
  private final String myName;
  private final String myDisplayableName;

  private ActionNameWrapper(String name) {
    myName = name;
    myDisplayableName = getDisplayableActionName(name);
  }

  private static String getDisplayableActionName(String name) {
    name = Util.NN(name).trim();
    name = name.replaceAll("(^|[^&])(&)($|[^&])", "$1&&$3");
    if (!name.endsWith(".") && !name.endsWith("\u2026")) name = name + "\u2026";
    return name;
  }

  @Override
  public void update(UpdateContext context) throws CantPerformException {
    context.putPresentationProperty(PresentationKey.ENABLE, EnableState.INVISIBLE);
    context.putPresentationProperty(PresentationKey.NAME, myDisplayableName);
    UpdateRequest update = context.getUpdateRequest();
    listenContext(context, update);
    WorkflowEditFeature object = chooseWorkflowObject(context);
    context.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
    object.listenIssues(context);
  }

  private static void listenContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    updateRequest.watchRole(LoadedItem.ITEM_WRAPPER);
    updateRequest.updateOnChange(context.getSourceObject(WLActionSets.ROLE).getModifiable());
    updateRequest.updateOnChange(context.getSourceObject(JiraProvider3.ROLE).getWorkflowActions().getModifiable());
  }

  @NotNull
  private WorkflowEditFeature chooseWorkflowObject(ActionContext context) throws CantPerformException {
    Pair<JiraConnection3,List<ItemWrapper>> pair = JiraEditUtils.selectIssuesWrappers(context);
    JiraConnection3 connection = pair.getFirst();
    CantPerformException.ensure(connection.isUploadAllowed());
    ActionApplication application = CantPerformException.ensureNotNull(ActionApplication.create(pair.getSecond(), connection, myName));
    return new WorkflowEditFeature(application, pair.getSecond());
  }

  @Override
  public void perform(ActionContext context) throws CantPerformException {
    EditItemAction.startEdit(context, chooseWorkflowObject(context));
  }

  @Override
  public Object getIdentity() {
    return myName;
  }

  public static AListModel<IdentifiableAction> createModel(Lifespan life, AListModel<String> actionNames) {
    return FilteringConvertingListDecorator.create(life, actionNames, null, new Convertor<String, IdentifiableAction>() {
      @Override
      public IdentifiableAction convert(String value) {
        return new ActionNameWrapper(value);
      }
    });
  }
}
