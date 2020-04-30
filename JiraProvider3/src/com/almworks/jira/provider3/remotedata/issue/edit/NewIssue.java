package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.remotedata.issue.EditRequest;
import com.almworks.jira.provider3.remotedata.issue.ParsedIssueFields;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.*;

class NewIssue extends CreateIssueUnit {
  private static final LocalizedAccessor.Value M_NO_PROJECT_TYPE_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.submit.noProjectType.short");
  private static final LocalizedAccessor.Value M_NO_PROJECT_TYPE_FULL = PrepareIssueUpload.I18N.getFactory("upload.problem.submit.noProjectType.full");
  private static final LocalizedAccessor.Value M_OPERATION_NAME = PrepareIssueUpload.I18N.getFactory("upload.operation.submitIssue");
  private final NotConfirmedSubmits mySubmitSupport;
  @Nullable
  private final Date myCheckCreated;
  @Nullable
  private final CreateIssueUnit myParentItem;
  @Nullable
  private Integer myIssueId;
  @Nullable
  private String myKey;
  private boolean myFailed = false;

  NewIssue(long item, NotConfirmedSubmits submitSupport, @Nullable Date checkCreated, @Nullable CreateIssueUnit parentItem) {
    super(item);
    mySubmitSupport = submitSupport;
    myCheckCreated = checkCreated;
    myParentItem = parentItem;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    if (BEFORE_UPLOAD.equals(purpose)) {
      if (myCheckCreated == null) return null; // No previous attempt was made - nothing to check
      if (myIssueId == null) return mySubmitSupport.checkFailedSubmits(session, transaction, context);
      else return doLoadServerState(session, transaction, context, purpose);
    } else return doLoadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException {
    if (myFailed || myIssueId != null) {
      LogHelper.error("Already complete", myFailed, myIssueId);
      return Collections.emptyList();
    }
    EditIssue edit = getEdit();
    if (edit == null) return UploadProblem.internalError().toCollection();
    Integer parentId;
    if (myParentItem != null) {
      parentId = myParentItem.getIssueId();
      if (parentId == null) return UploadProblem.notNow("Parent not submitted " + myParentItem).toCollection();
    } else parentId = null;
    List<IssueFieldValue> values = edit.getValues();
    Integer projectId =findChangeId(IssueFields.PROJECT, values);
    Integer typeId = findChangeId(IssueFields.ISSUE_TYPE, values);
    if (projectId == null || typeId == null) return UploadProblem.illegalData(M_NO_PROJECT_TYPE_SHORT.create(), M_NO_PROJECT_TYPE_FULL.create()).toCollection();
    ParsedIssueFields meta = ParsedIssueFields.loadCreateMeta(session, projectId, typeId);
    JSONObject fields = new JSONObject();
    fields.put("project", UploadJsonUtil.object("id", projectId.toString()));
    fields.put("issuetype", UploadJsonUtil.object("id", typeId.toString()));
    if (parentId != null) fields.put(ServerFields.PARENT.getJiraId(), UploadJsonUtil.object("id", parentId.toString()));
    RestServerInfo serverInfo = RestServerInfo.get(session);
    EditRequest request = new EditRequest(meta, true, serverInfo);
    UploadProblem problem = request.fillFields(values);
    if (problem != null) return problem.toCollection();
    RestResponse response = null;
    ConnectorException exception = null;
    JSONObject submit = new JSONObject();
    submit.put("fields", fields);
    request.addUpdate(submit);
    try {
      response = session.restPostJson("api/2/issue", submit, RequestPolicy.NEEDS_LOGIN);
    } catch (ConnectorException e) {
      exception = e;
    }
    boolean success = request.processResponse(this, response, exception, M_OPERATION_NAME.create());
    if (success && response != null) {
      try {
        JSONObject issue = response.getJSONObject();
        Integer id = JRIssue.ID.getValue(issue);
        String key = JRIssue.KEY.getValue(issue);
        if (id == null || key == null) {
          LogHelper.error("Failed to get submitted issue id, key", getIssueItem(), id, key);
          success = false;
        } else {
          myIssueId = id;
          myKey = key;
        }
      } catch (ParseException e) {
        LogHelper.warning("Failed parse submit issue confirmation");
        success = false;
        request.addProblem(UploadProblem.parseProblem(M_OPERATION_NAME.create()));
      }
    }
    myFailed = !success;
    return request.getProblems();
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (myIssueId != null) {
      EntityHolder issue = ServerIssue.findIssue(transaction, myIssueId);
      if (issue != null) {
        issue.setItem(getIssueItem());
        context.reportUploaded(getIssueItem(), SyncAttributes.INVISIBLE);
        context.reportUploaded(getIssueItem(), IssueFields.PROJECT.getAttribute());
        context.reportUploaded(getIssueItem(), IssueFields.ISSUE_TYPE.getAttribute());
        context.reportUploaded(getIssueItem(), IssueFields.STATUS.getAttribute());
        context.reportUploaded(getIssueItem(), Issue.PARENT);
      } else LogHelper.error("Submitted issue was not found", myIssueId, getIssueItem());
    }
  }

  @Override
  public boolean isDone() {
    return myIssueId != null;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return myFailed || getEdit() == null;
  }

  @Nullable
  @Override
  public Integer getIssueId() {
    return myIssueId;
  }

  @Nullable
  @Override
  public String getIssueKey() {
    return myKey;
  }

  @Override
  public void issueKeyUpdated(String key) {
    myKey = key;
  }

  @Nullable
  Date getPrevFailure() {
    return myCheckCreated;
  }

  @SuppressWarnings("RedundantIfStatement")
  public boolean matches(Date created, String summary, int projectId, int typeId, @Nullable Integer parentId) {
    EditIssue edit = getEdit();
    if (myIssueId != null || myKey != null || myCheckCreated == null || edit == null) {
      LogHelper.error("Already submitted or not failed", myIssueId, myKey, myCheckCreated, edit);
      return false;
    }
    if (myParentItem != null) {
      if (parentId == null) return false;
      Integer ownParentId = myParentItem.getIssueId();
      if (ownParentId == null) return false; // todo maybe parent failed too
      if (!parentId.equals(ownParentId)) return false;
    } else if (parentId != null) return false;
    if (myCheckCreated.after(created)) return false;
    List<IssueFieldValue> values = edit.getValues();
    if (!Util.equals(projectId, findChangeId(IssueFields.PROJECT, values))) return false;
    if (!Util.equals(typeId, findChangeId(IssueFields.ISSUE_TYPE, values))) return false;
    if (!Util.equals(summary, IssueFields.SUMMARY.findChangeValue(values))) return false;
    return true;
  }

  void submittedFound(JSONObject issue) {
    if (myIssueId != null || myKey != null) {
      LogHelper.error("Already submitted", myIssueId, myKey, issue.toJSONString());
      return;
    }
    Integer id = JRIssue.ID.getValue(issue);
    String key = JRIssue.KEY.getValue(issue);
    if (id == null || key == null) {
      LogHelper.error("Missing issue identity", id, key, this);
      return;
    }
    myIssueId = id;
    myKey = key;
  }
}
