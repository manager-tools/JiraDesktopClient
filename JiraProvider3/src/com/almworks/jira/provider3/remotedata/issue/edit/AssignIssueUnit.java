package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.download2.rest.LoadedEntity;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Map;

class AssignIssueUnit implements UploadUnit {
  private static final LocalizedAccessor.Value M_USER_UNASSIGNED = PrepareIssueUpload.I18N.getFactory("upload.assign.user.unassigned");
  private static final LocalizedAccessor.Value M_FAILURE_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.assign.failure.short");
  private static final LocalizedAccessor.MessageStr M_FAILURE_400 = PrepareIssueUpload.I18N.messageStr("upload.problem.assign.failure.400");
  private static final LocalizedAccessor.MessageIntStr M_FAILURE_NO_PERMISSION = PrepareIssueUpload.I18N.messageIntStr("upload.problem.assign.failure.401_403");
  private static final LocalizedAccessor.MessageStr M_FAILURE_404 = PrepareIssueUpload.I18N.messageStr("upload.problem.assign.failure.404");
  private static final LocalizedAccessor.MessageIntStr M_FAILURE_GENERIC = PrepareIssueUpload.I18N.messageIntStr("upload.problem.assign.failure.generic");
  private final EditIssue myIssue;

  public AssignIssueUnit(EditIssue issue) {
    myIssue = issue;
  }

  @Override
  public boolean isDone() {
    return findNotDoneAssign() == null;
  }

  private EntityFieldDescriptor.MyValue findNotDoneAssign() {
    EntityFieldDescriptor.MyValue value = IssueFields.ASSIGNEE.findValue(myIssue.getValues());
    return value == null || !value.isChanged() || value.isDone() ? null : value;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return context.isFailed(myIssue.getCreate()) || myIssue.isConflictDetected(); // Allow to assign issues even when general edit failed.
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException
  {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    return null; // EditIssue does all the job
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    if (UploadUnitUtils.waitPostEdit(context, myIssue)) return UploadProblem.notNow("Wait for edit issue").toCollection();
    Integer issueId = myIssue.getCreate().getIssueId();
    if (issueId == null) return UploadProblem.notNow("Issue is not submitted").toCollection();
    EntityFieldDescriptor.MyValue value = findNotDoneAssign();
    if (value == null) return null; // Nothing to upload
    Object userObj = value.getJsonValue();
    JSONObject user = Util.castNullable(JSONObject.class, userObj);
    if (user == null) {
      LogHelper.error("Wrong REST presentation", userObj);
      throw UploadProblem.internalError().toException();
    }
    RestResponse response = session.restPut("api/2/issue/" + issueId + "/assignee", user, RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) {
      LogHelper.warning("Failed to assign issue", myIssue.getCreate(), user);
      String shortText = M_FAILURE_SHORT.create();
      LoadedEntity change = value.getChange();
      String displayableUser = change == null ? M_USER_UNASSIGNED.create() : change.getDisplayableText();
      String longText;
      int statusCode = response.getStatusCode();
      switch (statusCode) {
      case 400: longText = M_FAILURE_400.formatMessage(displayableUser); break;
      case 401:
      case 403:
        longText = M_FAILURE_NO_PERMISSION.formatMessage(statusCode, displayableUser); break;
      case 404: longText = M_FAILURE_404.formatMessage(displayableUser); break;
      default: longText = M_FAILURE_GENERIC.formatMessage(statusCode, displayableUser); break;
      }
      return UploadProblem.fatal(shortText, longText).toCollection();
    }
    value.uploadDone(true);
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    // EditIssue (values) does all job
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }
}
