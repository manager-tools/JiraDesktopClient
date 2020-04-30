package com.almworks.jira.provider3.workflow;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.edit.workflow.WorkflowStep;
import com.almworks.jira.provider3.remotedata.issue.BaseHistoryUnit;
import com.almworks.jira.provider3.remotedata.issue.EditRequest;
import com.almworks.jira.provider3.remotedata.issue.ParsedIssueFields;
import com.almworks.jira.provider3.remotedata.issue.StepLoader;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Status;
import com.almworks.jira.provider3.schema.WorkflowAction;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.download2.rest.JRStatus;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerStatus;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.*;

public class UploadWorkflowUnit extends BaseHistoryUnit {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(PrepareIssueUpload.class.getClassLoader(), "com/almworks/jira/provider3/workflow/message");
  private static final LocalizedAccessor.MessageStr M_CONFLICT_SHORT = I18N.messageStr("upload.problem.workflow.conflict.short");
  private static final LocalizedAccessor.Message3 M_CONFLICT_FULL = I18N.message3("upload.problem.workflow.conflict.full");
  private static final LocalizedAccessor.MessageStr M_NOTHING_CHANGED_SHORT = I18N.messageStr("upload.problem.workflow.nothingChanged.short");
  private static final LocalizedAccessor.MessageStr M_NOTHING_CHANGED_FULL = I18N.messageStr("upload.problem.workflow.nothingChanged.full");

  public static final StepLoader LOADER = new StepLoader() {
    @Override
    public UploadUnit loadStep(ItemVersion trunk, HistoryRecord record, CreateIssueUnit create, LoadUploadContext context, @Nullable UploadUnit prevStep, int stepIndex) {
      WorkflowStep step = WorkflowStep.load(trunk.getReader(), record.getDataStream());
      if (step == null) return null;
      ItemVersion action = trunk.forItem(step.getAction());
      Integer id = action.getValue(WorkflowAction.ID);
      String name = WorkflowAction.NAME.getNNValue(action, "").trim();
      ItemVersion statusItem = trunk.forItem(step.getExpectedStatus());
      if (id == null || statusItem.getItem() <= 0) return null;
      if (name.length() == 0) name = "Workflow action";
      name = name + " (" + id + ")";
      ArrayList<IssueFieldValue> values = EditIssue.loadValues(context, step.getState(), trunk.switchToServer());
      Integer statusId = statusItem.getValue(Status.ID);
      String statusName = statusItem.getValue(Status.NAME);
      Entity status = ServerStatus.create(statusId, statusName);
      return new UploadWorkflowUnit(prevStep, create, stepIndex, id, values, name, status);
    }
  };

  private final int myActionId;
  private final List<IssueFieldValue> myValues;
  private final String myDisplayableName;
  private final Entity myExpectedStatus;

  private UploadWorkflowUnit(@Nullable UploadUnit prevStep, CreateIssueUnit issue, int stepIndex, int actionId, List<IssueFieldValue> values, String displayableName, Entity expectedStatus) {
    super(prevStep, issue, stepIndex);
    myActionId = actionId;
    myValues = values;
    myDisplayableName = displayableName;
    myExpectedStatus = expectedStatus;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException
  {
    EditRequest.ensureHasServerInfo(context, session);
    return super.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    if (!isFirstStep()) return null; // Previous history step result can not be known yet
    EntityHolder issue = getIssue().findIssue(transaction);
    if (issue == null) {
      LogHelper.error("No issue loaded", getIssue());
      return UploadProblem.internalError();
    }
    EntityHolder actualStatus = issue.getReference(ServerIssue.STATUS);
    if (actualStatus == null) {
      LogHelper.error("Missing issue status", issue);
      return UploadProblem.internalError();
    }
    if (Util.equals(myExpectedStatus.get(ServerStatus.ID), actualStatus.getScalarValue(ServerStatus.ID))) return null; // Status matches
    String actualName = ServerStatus.getDisplayName(actualStatus);
    return conflict(actualName);
  }

  private UploadProblem conflict(String actualName) {
    String expectedName = ServerStatus.getDisplayName(myExpectedStatus);
    return UploadProblem.conflict(getIssue().getIssueItem(), M_CONFLICT_SHORT.formatMessage(myDisplayableName), M_CONFLICT_FULL.formatMessage(expectedName, actualName, myDisplayableName));
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId) throws ConnectorException,
    UploadProblem.Thrown {
    Pair<Integer, Date> initialState = checkInitialState(session, issueId);
    ParsedIssueFields fields = ParsedIssueFields.loadTransitionMeta(session, issueId, myActionId);

    Collection<? extends UploadProblem> problems = tryUpload(session, context, issueId, initialState, fields, false);
    if (problems != null && !problems.isEmpty()) problems = tryUpload(session, context, issueId, initialState, fields, true);
    if (problems != null) {
      if (problems.isEmpty()) markSuccess();
      return problems;
    }
    return UploadProblem.fatal(M_NOTHING_CHANGED_SHORT.formatMessage(myDisplayableName), M_NOTHING_CHANGED_FULL.formatMessage(myDisplayableName)).toCollection();
  }

  /**
   * @param loadFinalValues if set to true adds last edited values, otherwise uploads only values from workflow history step
   * @return not empty collection if upload failed with some problems, returns problems<br>
   *   empty collection if upload succeeded and issue has been changed<br>
   *   null if JIRA returns no error, but issue is not changed. <br>
   *   Also returns null if loadFinalValues is requested but no additional values is available for upload. Does not try to upload if no final values is available.
   */
  @Nullable("When no problem happen but issue is not changed")
  private Collection<? extends UploadProblem> tryUpload(RestSession session, UploadContext context, int issueId, Pair<Integer, Date> initialState, ParsedIssueFields fields, boolean loadFinalValues)
    throws UploadProblem.Thrown, ConnectorException {
    EditRequest request = new EditRequest(fields, false, EditRequest.getServerInfo(context));
    UploadProblem problem = request.fillFields(myValues);
    if (problem == null && loadFinalValues) {
      EditIssue edit = getIssue().getEdit();
      if (edit == null) return null;
      problem = request.fillFields(edit.getValues());
    }
    if (problem != null) return problem.toCollection();
    JSONObject object = new JSONObject();
    object.put("transition", UploadJsonUtil.object("id", myActionId));
    request.addUpdate(object);
    RestResponse response = null;
    ConnectorException failure;
    try {
      response = session.restPostJson("api/2/issue/" + issueId + "/transitions", object, RequestPolicy.NEEDS_LOGIN);
      failure = null;
    } catch (ConnectorException e) {
      failure = e;
    }
    if (request.processResponse(getIssue(), response, failure, myDisplayableName)) {
      Pair<JSONObject, Date> postState = loadIssue(session, issueId);
      JSONObject status = postState.getFirst();
      Integer id = status != null ? JRStatus.ID.getValue(status) : null;
      // See (https://jira.almworks.com/browse/JCO-1522) JCO-1522 Workflow action with validation upload: wrong change can be lost
      if (Util.equals(id, initialState.getFirst()) && Util.equals(postState.getSecond(), initialState.getSecond())) return null; // Issue is not changed
    }
    return request.getProblems();
  }

  @NotNull
  private Pair<Integer, Date> checkInitialState(RestSession session, int issueId) throws ConnectorException, UploadProblem.Thrown {
    Pair<JSONObject, Date> state = loadIssue(session, issueId);
    JSONObject status = state.getFirst();
    if (!isFirstStep() && !Util.equals(myExpectedStatus.get(ServerStatus.ID), status)) {
      String name = status != null ? JRStatus.NAME.getValue(status) : null;
      throw  conflict(name).toException();
    }
    Integer id = status != null ? JRStatus.ID.getValue(status) : null;
    return Pair.create(id, state.getSecond());
  }

  @NotNull
  private Pair<JSONObject, Date> loadIssue(RestSession session, int issueId) throws ConnectorException, UploadProblem.Thrown {
    RestResponse response = session.restGet("api/2/issue/" + issueId + "?fields=status,updated", RequestPolicy.SAFE_TO_RETRY);
    response.ensureSuccessful();
    JSONObject issue;
    try {
      issue = response.getJSONObject();
    } catch (ParseException e) {
      LogHelper.warning("Failied to parse issue (current status)");
      throw  UploadProblem.parseProblem(myDisplayableName).toException();
    }
    JSONObject status = JRIssue.STATUS.getValue(issue);
    Date updated = JRIssue.UPDATED.getValue(issue);
    return Pair.create(status, updated);
  }

  @Override
  public String toString() {
    return "UploadWorkflow(" + myActionId + "," + myDisplayableName + "," + myExpectedStatus + ")";
  }
}
