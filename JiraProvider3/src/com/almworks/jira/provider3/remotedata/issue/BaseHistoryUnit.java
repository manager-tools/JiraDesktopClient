package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import com.almworks.util.Pair;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public abstract class BaseHistoryUnit implements UploadUnit {
  @Nullable
  private final UploadUnit myPrevStep;
  private final CreateIssueUnit myIssue;
  private final int myStepIndex;
  private boolean mySuccess = false;

  protected BaseHistoryUnit(UploadUnit prevStep, CreateIssueUnit issue, int stepIndex) {
    myPrevStep = prevStep;
    myIssue = issue;
    myStepIndex = stepIndex;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    if ((myPrevStep != null && context.isFailed(myPrevStep)) || context.isFailed(myIssue)) return true;
    EditIssue edit = myIssue.getEdit();
    return edit != null && edit.isConflictDetected();
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }

  @Override
  public boolean isDone() {
    return mySuccess;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    if (myPrevStep != null && !myPrevStep.isDone()) return UploadProblem.notNow("Prev step not loaded " + myPrevStep).toCollection();
    Integer id = myIssue.getIssueId();
    if (id == null) return UploadProblem.notNow("Issue not submitted yet " + myIssue).toCollection();
    return doPerform(session, context, id);
  }

  protected abstract Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId) throws ConnectorException, UploadProblem.Thrown;

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (mySuccess) {
      context.reportHistory(getIssue().getIssueItem(), myStepIndex, this);
      reportAdditionalUpload(transaction, context, myIssue.getIssueItem());
    }
  }

  protected void reportAdditionalUpload(EntityTransaction transaction, PostUploadContext context, long issueItem) {}

  protected final boolean isFirstStep() {
    return myPrevStep == null;
  }

  protected final void markSuccess() {
    mySuccess = true;
  }

  @NotNull
  protected final CreateIssueUnit getIssue() {
    return myIssue;
  }
}
