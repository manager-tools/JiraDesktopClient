package com.almworks.jira.provider3.remotedata.issue.misc;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class VoteIssueUnit implements UploadUnit {
  private static final LocalizedAccessor.Value M_VOTE_FAILED_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.vote.failed.add.short");
  private static final LocalizedAccessor.Value M_VOTE_FAILED_FULL = PrepareIssueUpload.I18N.getFactory("upload.problem.vote.failed.add.full");
  private static final LocalizedAccessor.Value M_REMOVE_VOTE_FAILED_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.vote.failed.remove.short");
  private static final LocalizedAccessor.Value M_REMOVE_VOTE_FAILED_FULL = PrepareIssueUpload.I18N.getFactory("upload.problem.vote.failed.remove.full");

  private final boolean myVote;
  private final EditIssue myIssue;
  private boolean myDone = false;

  public VoteIssueUnit(EditIssue editIssue, boolean voted) {
    myIssue = editIssue;
    myVote = voted;
  }

  @Override
  public boolean isDone() {
    return myDone;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return UploadUnitUtils.isPreEditFailed(context, myIssue);
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
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    if (isEqualVoted(transaction)) myDone = true;
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    Integer issueId = getCreateIssue().getIssueId();
    if (issueId == null) throw  UploadProblem.notNow("Issue not submitted yet").toException();
    String path = "api/2/issue/" + issueId + "/votes";
    RestResponse response;
    if (myVote) response = session.restPostJson(path, null, RequestPolicy.SAFE_TO_RETRY);
    else response = session.restDelete(path, RequestPolicy.SAFE_TO_RETRY);
    if (response.isSuccessful()) {
      myDone = true;
      return null;
    }
    LogHelper.debug("Vote issue failed", myVote, response.getStatusCode(), myIssue);
    return UploadProblem.fatal((myVote ? M_VOTE_FAILED_SHORT : M_REMOVE_VOTE_FAILED_SHORT).create(),
      (myVote ? M_VOTE_FAILED_FULL : M_REMOVE_VOTE_FAILED_FULL).create()).toCollection();
  }

  private CreateIssueUnit getCreateIssue() {
    return myIssue.getCreate();
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (!isDone()) return;
    if (isEqualVoted(transaction)) {
      long issue = getCreateIssue().getIssueItem();
      context.reportUploaded(issue, Issue.VOTED);
      context.reportUploaded(issue, Issue.VOTES_COUNT);
      context.reportUploaded(issue, Issue.VOTERS);
    } else
      LogHelper.debug("Vote issue not upload", getCreateIssue(), myVote);
  }

  private boolean isEqualVoted(EntityTransaction transaction) {
    Integer issueId = getCreateIssue().getIssueId();
    if (issueId == null) return false;
    EntityHolder issue = ServerIssue.findIssue(transaction, issueId);
    if (issue == null) return false;
    boolean isVoted = Boolean.TRUE.equals(issue.getScalarValue(ServerIssue.VOTED));
    return myVote == isVoted;
  }

  @NotNull
  public static Collection<? extends UploadUnit> load(EditIssue editIssue, ItemVersion item) {
    boolean voted = Boolean.TRUE.equals(item.switchToTrunk().getValue(Issue.VOTED));
    boolean wasVoted = Boolean.TRUE.equals(item.switchToServer().getValue(Issue.VOTED));
    if (voted == wasVoted) return Collections.emptyList();
    return Collections.singleton(new VoteIssueUnit(editIssue, voted));
  }
}
