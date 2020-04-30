package com.almworks.jira.provider3.attachments.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.remotedata.issue.UploadUnitUtils;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
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

public class DeleteAttachment implements UploadUnit {
  private static final LocalizedAccessor.Value M_FAILURE_SHORT = JiraAttachments.I18N.getFactory("attachment.upload.delete.failure.short");
  private static final LocalizedAccessor.Message2 M_FAILURE_PERMISSION = JiraAttachments.I18N.message2("attachment.upload.delete.failure.permission.full");
  private static final LocalizedAccessor.Message2 M_FAILURE_NOT_FOUND = JiraAttachments.I18N.message2("attachment.upload.delete.failure.notFound.full");
  private static final LocalizedAccessor.Message4 M_FAILURE_GENERIC = JiraAttachments.I18N.message4("attachment.upload.delete.failure.generic.full");
  private static final LocalizedAccessor.Message3 M_FAILURE_GENERIC_DETAILED = JiraAttachments.I18N.message3("attachment.upload.delete.failure.generic.detailed");
  private static final LocalizedAccessor.Message2 M_FAILURE_UNKNOWN = JiraAttachments.I18N.message2("attachment.upload.delete.failure.unknown.full");

  private final long myAttachmentItem;
  private final AttachmentValues myAttachment;
  private final CreateIssueUnit myIssue;
  private boolean myDone = false;
  private UploadProblem myFailure = null;

  public DeleteAttachment(long attachmentItem, AttachmentValues attachment, CreateIssueUnit issue) {
    myAttachmentItem = attachmentItem;
    myAttachment = attachment;
    myIssue = issue;
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
    EntityHolder attachment = myAttachment.find(transaction, myIssue);
    if (attachment == null) myDone = true;
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    UploadProblem notNow = UploadUnitUtils.checkEditNotDoneYet(myIssue, context);
    if (notNow != null) return notNow.toCollection();
    Integer id = myAttachment.getId();
    Integer issueId = myIssue.getIssueId();
    if (id == null || issueId == null) return null;
    boolean success = false;
    try {
      RestResponse response = session.restDelete("api/2/attachment/" + id, RequestPolicy.NEEDS_LOGIN);
      if (response.isSuccessful()) success = true;
      else {
        String failureShort = M_FAILURE_SHORT.create();
        String attachName = myAttachment.getAttachmentName();
        String author = myAttachment.getAuthorName();
        int code = response.getStatusCode();
        LogHelper.warning("Failed to delete attachment", attachName, myAttachment.getId(), code);
        if (code == 403) myFailure = UploadProblem.fatal(failureShort, M_FAILURE_PERMISSION.formatMessage(attachName, author));
        else if (code == 404) myFailure = UploadProblem.fatal(failureShort, M_FAILURE_NOT_FOUND.formatMessage(attachName, author));
        else {
          RestResponse.ErrorResponse error = response.createErrorResponse();
          String description = error.hasDetails() ? M_FAILURE_GENERIC_DETAILED.formatMessage(attachName, author, error.getFullMessage()) :
                  M_FAILURE_GENERIC.formatMessage(attachName, author, String.valueOf(code), response.getStatusText());
          myFailure = UploadProblem.fatal(failureShort, description);
        }
        success = true;
      }
    } finally {
      myDone = true;
      if (!success) {
        LogHelper.warning("Unknown problem deleting attachment", myAttachment.getAuthorName(), myAttachment.getId());
        myFailure = UploadProblem.fatal(M_FAILURE_SHORT.create(), M_FAILURE_UNKNOWN.formatMessage(myAttachment.getAttachmentName(), myAttachment.getAuthorName()));
      }
    }
    return null;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (myAttachment.find(transaction, myIssue) == null) context.reportUploaded(myAttachmentItem, SyncSchema.INVISIBLE);
    else if (myFailure != null) context.addMessage(this, myFailure);
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myIssue.getMasterItems();
  }
}
