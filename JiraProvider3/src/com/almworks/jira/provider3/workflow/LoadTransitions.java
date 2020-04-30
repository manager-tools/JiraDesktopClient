package com.almworks.jira.provider3.workflow;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.remotedata.issue.ParsedIssueFields;
import com.almworks.jira.provider3.sync.download2.meta.CustomFieldOptionsCollector;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRStatus;
import com.almworks.jira.provider3.sync.download2.rest.JRTransition;
import com.almworks.jira.provider3.sync.schema.ServerActionSet;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerWorkflowAction;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.restconnector.json.sax.PeekArrayElement;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class LoadTransitions {
  private static final LocalizedAccessor.Value M_PROGRESS = UploadWorkflowUnit.I18N.getFactory("load.transitions.progress");

  private final RestSession mySession;
  private final EntityHolder myIssue;
  private final CustomFieldOptionsCollector myFieldOptions;

  public LoadTransitions(RestSession session, EntityHolder issue, CustomFieldsComponent customFields) {
    mySession = session;
    myIssue = issue;
    myFieldOptions = new CustomFieldOptionsCollector(customFields);
  }

  public void perform(ProgressInfo progress) throws ConnectorException {
    progress.startActivity(M_PROGRESS.create());
    final List<ActionInfo> actionInfos = downloadTransitions(progress);
    progress.setDone();
    if (actionInfos == null) return;
    EntityHolder project = myIssue.getReference(ServerIssue.PROJECT);
    EntityHolder type = myIssue.getReference(ServerIssue.ISSUE_TYPE);
    if (project == null || type == null) {
      LogHelper.error("Missing data", project, type);
      return;
    }
    EntityTransaction transaction = myIssue.getTransaction();
    List<EntityHolder> applicableActions = Collections15.arrayList();
    for (ActionInfo actionInfo : actionInfos) {
      EntityHolder action = ServerWorkflowAction.create(transaction, actionInfo.myId, project, type);
      if (action == null) continue;
      List<EntityHolder> allFields = Collections15.arrayList();
      List<EntityHolder> mandatoryFields = Collections15.arrayList();
      for (ParsedIssueFields.Info info : actionInfo.getFields()) {
        EntityHolder field = info.getFieldEntity(transaction);
        if (field != null) {
          allFields.add(field);
          if (info.isRequired()) mandatoryFields.add(field);
        }
      }
      action.setNNValue(ServerWorkflowAction.NAME, actionInfo.myName);
      action.setNNValue(ServerWorkflowAction.TARGET_STATUS, actionInfo.myToStatus);
      action.setReferenceCollection(ServerWorkflowAction.FIELDS, allFields);
      action.setReferenceCollection(ServerWorkflowAction.MANDATORY_FIELDS, mandatoryFields);
      applicableActions.add(action);
      myFieldOptions.postProcess(transaction, false);
    }
    myIssue.setReferenceCollection(ServerIssue.APPLICABLE_WORKFLOW_ACTIONS, applicableActions);
    EntityHolder actionSet = ServerActionSet.findNewSet(myIssue);
    if (actionSet != null) actionSet.setReferenceCollection(ServerActionSet.ACTIONS, applicableActions);
  }

  @Nullable
  private List<ActionInfo> downloadTransitions(ProgressInfo progress) throws ConnectorException {
    Integer issueId = myIssue.getScalarValue(ServerIssue.ID);
    if (issueId == null)
      return null;
    RestResponse response = mySession.restGet("api/2/issue/" + issueId + "/transitions?expand=transitions.fields", RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) {
      progress.addHttpStatusError(response.getLastUrl(), response.getStatusCode(), response.getStatusText());
      return null;
    }
    final List<ActionInfo> actionInfos = Collections15.arrayList();
    response.parseJSON(PeekArrayElement.entryArray("transitions", JSONCollector.objectConsumer(transition -> {
      Integer id = JRTransition.ID.getValue(transition);
      if (id == null)
        LogHelper.warning("Missing action id", transition);
      else {
        ParsedIssueFields fields = new ParsedIssueFields();
        fields.addFieldProcessor(myFieldOptions::addFieldOptions);
        JSONCollector.sendObject(transition, fields.createFieldsHandler());
        String name = JRTransition.NAME.getValue(transition);
        Entity status = JRStatus.JSON_CONVERTOR.convert(JRTransition.TO_STATUS.getValue(transition));
        actionInfos.add(new ActionInfo(id, name, status, fields));
      }
    })));
    return actionInfos;
  }

  private static class ActionInfo {
    private final int myId;
    private final String myName;
    private final Entity myToStatus;
    private final ParsedIssueFields myFields;

    private ActionInfo(int id, String name, Entity toStatus, ParsedIssueFields fields) {
      myId = id;
      myName = name;
      myToStatus = toStatus;
      myFields = fields;
    }

    public Collection<ParsedIssueFields.Info> getFields() {
      return myFields.getFields().values();
    }
  }
}
