package com.almworks.jira.provider3.comments;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

class DeleteComment implements UploadUnit {
  private static final LocalizedAccessor.MessageIntStr M_FAILED_SHORT = PrepareCommentUpload.I18N.messageIntStr("comment.upload.delete.failure.short");
  private static final LocalizedAccessor.MessageIntStr M_FAILED_ERROR_CODE_FULL = PrepareCommentUpload.I18N.messageIntStr("comment.upload.delete.failure.errorCode");
  private static final LocalizedAccessor.Value M_FAILED_UNKNOWN_FULL = PrepareCommentUpload.I18N.getFactory("comment.upload.delete.failure.unknown");

  private final long myCommentItem;
  private final CreateIssueUnit myIssue;
  private final CommentValues myComment;
  private boolean myDone;
  private UploadProblem myFailure = null;

  public DeleteComment(long commentItem, CreateIssueUnit issue, CommentValues comment) {
    myCommentItem = commentItem;
    myIssue = issue;
    myComment = comment;
  }

  @Override
  public boolean isDone() {
    return myDone;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return false;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose)
    throws ConnectorException
  {
    return myIssue.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    EntityHolder comment = myComment.find(transaction, myIssue);
    if (comment == null) myDone = true;
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    UploadProblem notNow = UploadUnitUtils.checkEditNotDoneYet(myIssue, context);
    if (notNow != null) return notNow.toCollection();
    Integer id = myComment.getId();
    Integer issueId = myIssue.getIssueId();
    if (id == null || issueId == null) return null;
    boolean success = false;
    try {
      RestResponse response = session.restDelete("api/2/issue/" + issueId + "/comment/" + id, RequestPolicy.NEEDS_LOGIN);
      if (!response.isSuccessful()) {
        myFailure = UploadProblem.fatal(M_FAILED_SHORT.formatMessage(id, myComment.getAuthorName()), M_FAILED_ERROR_CODE_FULL.formatMessage(response.getStatusCode(), response.getStatusText()));;
        success = true;
      }
    } finally {
      myDone = true;
      if (!success) myFailure = UploadProblem.fatal(M_FAILED_SHORT.formatMessage(id, myComment.getAuthorName()), M_FAILED_UNKNOWN_FULL.create());
    }
    return null;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (myComment.find(transaction, myIssue) == null) context.reportUploaded(myCommentItem, SyncSchema.INVISIBLE);
    else if (myFailure != null) context.addMessage(this, myFailure);
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }
}
