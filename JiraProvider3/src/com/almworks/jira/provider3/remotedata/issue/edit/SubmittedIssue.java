package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

class SubmittedIssue extends CreateIssueUnit {
  @NotNull
  private final Integer myIssueId;
  private String myIssueKey;

  public SubmittedIssue(long item, int issueId, String issueKey) {
    super(item);
    myIssueId = issueId;
    myIssueKey = issueKey;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    return doLoadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    return null;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return false;
  }

  @NotNull
  @Override
  public Integer getIssueId() {
    return myIssueId;
  }

  @Override
  @NotNull
  public String getIssueKey() {
    return myIssueKey;
  }

  @Override
  public void issueKeyUpdated(String key) {
    myIssueKey = key;
  }
}
