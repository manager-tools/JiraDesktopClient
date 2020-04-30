package com.almworks.jira.provider3.comments;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.SlaveValues;
import com.almworks.jira.provider3.remotedata.issue.VisibilityLevel;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.util.LogHelper;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Date;

class CommentValues extends SlaveValues {
  private final String myText;
  @Nullable("When no visibility")
  private final VisibilityLevel myVisibility;
  private final String myAuthorName;
  private final Date myCreated;

  private CommentValues(Integer id, String text, VisibilityLevel visibility, String authorName, Date created) {
    super(id);
    myText = text;
    myVisibility = visibility;
    myAuthorName = authorName;
    myCreated = created;
  }

  public static CommentValues load(ItemVersion comment) throws UploadUnit.CantUploadException {
    Integer id = comment.getValue(Comment.ID);
    String text = comment.getValue(Comment.TEXT);
    if (text == null || text.isEmpty()) throw UploadUnit.CantUploadException.create("Empty comment text", comment);
    VisibilityLevel visibility = VisibilityLevel.load(comment.readValue(Comment.LEVEL));
    Date created = comment.getValue(Comment.CREATED);
    if (created == null) created = new Date();
    String authorName = AddEditSlaveUnit.loadAuthor(comment.readValue(Comment.AUTHOR));
    return new CommentValues(id, text, visibility, authorName, created);
  }

  @Nullable
  public EntityHolder find(@Nullable EntityHolder issue) {
    Integer id = getId();
    if (id == null || issue == null) return null;
    Integer issueId = issue.getScalarValue(ServerIssue.ID);
    if (issueId == null) return null;
    return ServerComment.find(issue.getTransaction(), issueId, id);
  }

  @Nullable
  public EntityHolder find(EntityTransaction transaction, CreateIssueUnit issueUnit) {
    EntityHolder issue = issueUnit.findIssue(transaction);
    return find(issue);
  }

  public boolean checkServer(EntityHolder comment) {
    String serverText = comment.getScalarValue(ServerComment.TEXT);
    if (!myText.equals(serverText) || !VisibilityLevel.areSame(comment.getReference(ServerComment.SECURITY), myVisibility)) {
      LogHelper.debug("Comment conflict", getId());
      return false;
    }
    return true;
  }

  @NotNull
  public String getAuthorName() {
    return myAuthorName;
  }

  @NotNull
  public Date getCreatedDate() {
    return myCreated;
  }

  public String messageAbout(LocalizedAccessor.Message2 message) {
    String author = getAuthorName();
    Date created = getCreatedDate();
    return message.formatMessage(author, DateUtil.toLocalDateOrTime(created));
  }

  @SuppressWarnings("unchecked")
  public JSONObject createJson() {
    JSONObject object = new JSONObject();
    object.put("body", myText);
    object.put("visibility", myVisibility != null ? myVisibility.createJson() : null);
    return object;
  }

  @Override
  public boolean matchesFailure(EntityHolder slave, @NotNull String thisUser) {
    return thisUser.equals(slave.getScalarFromReference(ServerComment.AUTHOR, ServerUser.ID)) && Util.equals(slave.getScalarValue(ServerComment.TEXT), myText);
  }
}
