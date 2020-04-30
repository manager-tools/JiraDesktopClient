package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.remotedata.issue.BaseHistoryUnit;
import com.almworks.jira.provider3.remotedata.issue.EditRequest;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;

import java.util.ArrayList;
import java.util.Map;

abstract class BaseMoveUnit extends BaseHistoryUnit {
  protected static final LocalizedAccessor.Value M_CANT_START_SHORT = MoveLoader.I18N.getFactory("upload.problem.move.cantStart.short");
  protected static final LocalizedAccessor.Value M_CANT_START_FULL = MoveLoader.I18N.getFactory("upload.problem.move.cantStart.full");
  private final MoveWizard myMoveType;
  private final boolean myReportParent;

  protected BaseMoveUnit(UploadUnit prevStep, CreateIssueUnit issue, int stepIndex, MoveWizard moveType, boolean reportParent) {
    super(prevStep, issue, stepIndex);
    myMoveType = moveType;
    myReportParent = reportParent;
  }

  protected static UploadProblem cantStartProblem() {
    return UploadProblem.fatal(M_CANT_START_SHORT.create(), M_CANT_START_FULL.create());
  }

  @Override
  protected void reportAdditionalUpload(EntityTransaction transaction, PostUploadContext context, long issueItem) {
    context.reportUploaded(issueItem, Issue.PROJECT);
    context.reportUploaded(issueItem, Issue.ISSUE_TYPE);
    context.reportUploaded(issueItem, Issue.STATUS);
    if (myReportParent) context.reportUploaded(issueItem, Issue.PARENT);
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
    return null;
  }

  public void addWarning(String warning) {
    myMoveType.addWarning(warning);
  }

  protected void finishMove(HtmlWizard wizard, ArrayList<IssueFieldValue> values, UploadContext context) throws UploadProblem.Thrown, ConnectorException {
    RestServerInfo serverInfo = EditRequest.getServerInfo(context);
    myMoveType.maybeUpdateStatus(wizard, values, serverInfo);
    myMoveType.updateFields(wizard, values, getIssue().getEdit(), serverInfo);
    String key = myMoveType.confirmMove(wizard);
    getIssue().issueKeyUpdated(key);
  }
}
