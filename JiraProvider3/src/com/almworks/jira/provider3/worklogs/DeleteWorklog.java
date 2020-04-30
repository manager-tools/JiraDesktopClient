package com.almworks.jira.provider3.worklogs;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

class DeleteWorklog implements UploadUnit {
  private static final LocalizedAccessor.Value M_FAILED_SHORT = PrepareWorklogsUpload.I18N.getFactory("upload.failure.delete.short");
  private static final LocalizedAccessor.Message3 M_FAILED_FULL = PrepareWorklogsUpload.I18N.message3("upload.failure.delete.full");

  private final long myItem;
  private final CreateIssueUnit myIssue;
  private final WorklogValues myBase;
  private final WorklogSet mySet;
  private Boolean myDone;

  public DeleteWorklog(WorklogSet set, long item, CreateIssueUnit issue, WorklogValues base) {
    myItem = item;
    myIssue = issue;
    myBase = base;
    mySet = set;
  }

  @Override
  public boolean isDone() {
    return Boolean.TRUE.equals(myDone);
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return Boolean.FALSE.equals(myDone) || AddEditSlaveUnit.isEditFailed(context, myIssue);
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException
  {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    EntityHolder issue = myIssue.findIssue(transaction);
    EntityHolder worklog = myBase.find(issue);
    if (worklog == null) {
      myDone = true;
      mySet.markDone(this, false);
    } else if (!myBase.checkServer(worklog)) {
      LogHelper.debug("Worklog conflict", myBase);
      return PrepareWorklogsUpload.createConflict(myItem, myBase);
    }
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    Integer issueId = myIssue.getIssueId();
    EditIssue edit = myIssue.getEdit();
    if (issueId == null || (edit != null && edit.hasNotDone(context))) return UploadProblem.notNow("Issue not submitted yet").toCollection();
    myDone = false;
    RestResponse response = session.restDelete("api/2/issue/" + issueId + "/worklog/" + myBase.getId() + "?" + mySet.getUpdateEstimate(), RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) return UploadProblem.fatal(M_FAILED_SHORT.create(), myBase.messageAbout3(M_FAILED_FULL, String.valueOf(response.getStatusCode()))).toCollection();
    mySet.markDone(this, true);
    myDone = true;
    return null;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    EntityHolder issue = myIssue.findIssue(transaction);
    if (myBase.find(issue) == null) {
      AddEditWorklog.reportUploaded(context, myItem);
      mySet.finishUpload(context, issue, myItem);
    }
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }
}
