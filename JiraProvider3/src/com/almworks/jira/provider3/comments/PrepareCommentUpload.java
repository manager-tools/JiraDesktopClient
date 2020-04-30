package com.almworks.jira.provider3.comments;

import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.remotedata.issue.SlaveIds;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.CollectUploadContext;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;

import java.util.Collection;
import java.util.Collections;

public class PrepareCommentUpload implements UploadUnit.Factory {
  public static final PrepareCommentUpload INSTANCE = new PrepareCommentUpload();
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(PrepareCommentUpload.class.getClassLoader(), "com/almworks/jira/provider3/comments/message");

  @Override
  public void collectRelated(ItemVersion comment, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ItemVersion issue = comment.readValue(Comment.ISSUE);
    if (issue != null && issue.getValue(Issue.ID) == null) context.requestUpload(issue.getItem(), true);
  }

  @Override
  public Collection<? extends UploadUnit> prepare(ItemVersion comment, LoadUploadContext context) throws UploadUnit.CantUploadException {
    CreateIssueUnit issue = CreateIssueUnit.getExisting(comment.readValue(Comment.ISSUE), context);
    if (issue == null) return null;
    SyncState state = comment.getSyncState();
    UploadUnit unit;
    switch (state) {
    case NEW:
      SlaveIds knownComments = SlaveIds.markUpload(context, comment, ServerComment.ISSUE, ServerComment.TYPE, ServerComment.ID);
      CommentValues newComment = CommentValues.load(comment);
      unit = new AddEditComment(comment.getItem(), issue, null, newComment, knownComments);
      break;
    case EDITED:
      CommentValues change = CommentValues.load(comment);
      CommentValues base = CommentValues.load(comment.switchToServer());
      unit = new AddEditComment(comment.getItem(), issue, base, change, null);
      break;
    case LOCAL_DELETE:
      unit = new DeleteComment(comment.getItem(), issue, CommentValues.load(comment.switchToServer()));
      break;
    case SYNC:
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT:
    default:
      throw UploadUnit.CantUploadException.create("Not uploadable comment state", state, comment);
    }
    return Collections.singleton(unit);
  }

  public static void findFailedUploads(EntityWriter writer) {
    FailedComment.findFailedUploads(writer);
  }
}
