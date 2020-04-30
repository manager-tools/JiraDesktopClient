package com.almworks.jira.provider3.comments;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.SlaveIds;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.sync.download2.rest.JRComment;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.Collection;

class AddEditComment extends AddEditSlaveUnit<CommentValues> {
  private static final LocalizedAccessor.Value M_NOT_FOUND_SHORT = PrepareCommentUpload.I18N.getFactory("upload.conflict.notFound.short");
  private static final LocalizedAccessor.Message2 M_NOT_FOUND_FULL = PrepareCommentUpload.I18N.message2("upload.conflict.notFound.full");
  private static final LocalizedAccessor.Value M_CONFLICT_SHORT = PrepareCommentUpload.I18N.getFactory("upload.conflict.conflict.short");
  private static final LocalizedAccessor.Message2 M_CONFLICT_FULL = PrepareCommentUpload.I18N.message2("upload.conflict.conflict.full");
  private static final LocalizedAccessor.Value M_NOT_CONFIRMED = PrepareCommentUpload.I18N.getFactory("upload.create.notConfirmed.short");
  private static final LocalizedAccessor.MessageInt M_GENERIC_FAILURE = PrepareCommentUpload.I18N.messageInt("upload.genericFailure.short");
  private static final LocalizedAccessor.MessageStr M_GENERIC_FAILURE_DETAILED = PrepareCommentUpload.I18N.messageStr("upload.genericFailure.detailed");
  private static final LocalizedAccessor.Value M_COMMENT_UPLOAD = PrepareCommentUpload.I18N.getFactory("comment.upload.short");

  AddEditComment(long item, CreateIssueUnit issue, CommentValues base, CommentValues change, SlaveIds knownComments) {
    super(item, issue, base, change, knownComments);
  }

  @Override
  protected UploadProblem checkForConflict(@Nullable EntityHolder issue, @NotNull CommentValues base) {
    EntityHolder comment = base.find(issue);
    if (comment == null) return conflict(M_NOT_FOUND_SHORT.create(), base.messageAbout(M_NOT_FOUND_FULL));
    if (!base.checkServer(comment)) return conflict(M_CONFLICT_SHORT.create(), base.messageAbout(M_CONFLICT_FULL));
    return null;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, int issueId, @Nullable EditIssue edit, @Nullable CommentValues base, @NotNull CommentValues change) throws ConnectorException {
    RestResponse response = base == null ? submitComment(session, issueId, change) : editComment(session, issueId, change);
    if (response.isSuccessful()) {
      JSONObject reply;
      try {
        reply = response.getJSONObject();
      } catch (ParseException e) {
        LogHelper.warning("Upload comment failed to parse");
        return UploadProblem.parseProblem(M_COMMENT_UPLOAD.create()).toCollection();
      }
      Integer commentId = JRComment.ID.getValue(reply);
      if (commentId != null) {
        if (base == null) change.setId(commentId);
        return null;
      }
      return UploadProblem.fatal(M_NOT_CONFIRMED.create(), null).toCollection();
    }
    RestResponse.ErrorResponse error = response.createErrorResponse();
    LogHelper.warning("Comment upload failed", error.getFullMessage());
    String description = error.hasDetails() ? M_GENERIC_FAILURE_DETAILED.formatMessage(error.getFullMessage()) :
            M_GENERIC_FAILURE.formatMessage(response.getStatusCode());
    return UploadProblem.fatal(description, null).toCollection();
  }

  private RestResponse editComment(RestSession session, int issueId, CommentValues change) throws ConnectorException {
    Integer id = change.getId();
    if (id == null) return null;
    return session.restPut("api/2/issue/" + issueId + "/comment/" + id, change.createJson(), RequestPolicy.NEEDS_LOGIN);
  }

  private RestResponse submitComment(RestSession session, int issueId, CommentValues change) throws ConnectorException {
    return session.restPostJson("api/2/issue/" + issueId + "/comment", change.createJson(), RequestPolicy.NEEDS_LOGIN);
  }

  @Override
  protected void doFinishUpload(PostUploadContext context, EntityHolder issue, long item, CommentValues change, boolean newSlave) {
    EntityHolder comment = change.find(issue);
    if (comment != null) {
      if (newSlave) comment.setItem(item);
      context.reportUploaded(item, SyncSchema.INVISIBLE);
      context.reportUploaded(item, Comment.TEXT);
      context.reportUploaded(item, Comment.LEVEL);
    }
  }
}
