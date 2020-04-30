package com.almworks.jira.provider3.worklogs;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.SlaveValues;
import com.almworks.jira.provider3.remotedata.issue.VisibilityLevel;
import com.almworks.jira.provider3.remotedata.issue.fields.scalar.ScalarUploadType;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Date;

class WorklogValues extends SlaveValues {
  private final Date myStarted;
  private final Integer mySeconds;
  private final String myComment;
  private final VisibilityLevel myVisibility;
  private final String myAuthorName;
  private final Date myCreated;

  private WorklogValues(Integer id, Date started, Integer seconds, String comment, VisibilityLevel visibility, String authorName, Date created) {
    super(id);
    myStarted = started;
    mySeconds = seconds;
    myComment = comment;
    myVisibility = visibility;
    myAuthorName = authorName;
    myCreated = created;
  }

  public static WorklogValues load(ItemVersion worklog) throws UploadUnit.CantUploadException {
    Integer id = worklog.getValue(Worklog.ID);
    String comment = worklog.getValue(Worklog.COMMENT);
    Date started = worklog.getValue(Worklog.STARTED);
    Integer seconds = worklog.getValue(Worklog.TIME_SECONDS);
    VisibilityLevel visibility = VisibilityLevel.load(worklog.readValue(Worklog.SECURITY));
    if (started == null || seconds == null) throw UploadUnit.CantUploadException.create("Missing worklog data", worklog, started, seconds);
    Date created = worklog.getValue(Worklog.CREATED);
    if (created == null) created = new Date();
    String authorName = AddEditSlaveUnit.loadAuthor(worklog.readValue(Worklog.AUTHOR));
    return new WorklogValues(id, started, seconds, comment, visibility, authorName, created);
  }

  @Override
  public boolean matchesFailure(EntityHolder slave, @NotNull Entity thisUser) {
    return ServerUser.sameUser(thisUser, slave.getReference(ServerWorklog.AUTHOR))
      && Util.equals(slave.getScalarValue(ServerWorklog.START_DATE), myStarted);
  }

  @Nullable("When not new, found or no issue")
  public EntityHolder find(@Nullable EntityHolder issue) {
    if (issue == null) return null;
    Integer id = getId();
    if (id == null) return null;
    Integer issueId = issue.getScalarValue(ServerIssue.ID);
    if (issueId == null) return null;
    return ServerWorklog.find(issue.getTransaction(), issueId, id);
  }

  public boolean checkServer(EntityHolder worklog) {
    Date start = worklog.getScalarValue(ServerWorklog.START_DATE);
    Integer seconds = worklog.getScalarValue(ServerWorklog.TIME_SECONDS);
    String comment = worklog.getScalarValue(ServerWorklog.COMMENT);
    EntityHolder visibility = worklog.getReference(ServerWorklog.SECURITY);
    return Util.equals(myStarted, start) && Util.equals(mySeconds, seconds) && Util.equals(myComment, comment) && VisibilityLevel.areSame(visibility, myVisibility);
  }

  @SuppressWarnings("unchecked")
  public JSONObject createJson() {
    JSONObject result = new JSONObject();
    result.put("comment", myComment);
    result.put("visibility", myVisibility != null ? myVisibility.createJson() : null);
    result.put("started", ScalarUploadType.DATE.toJsonValue(myStarted));
    result.put("timeSpentSeconds", mySeconds);
    Integer id = getId();
    if (id != null) result.put("id", id);
    return result;
  }

  public String messageAbout(LocalizedAccessor.Message2 message) {
    return message.formatMessage(myAuthorName, DateUtil.toLocalDateOrTime(myCreated));
  }

  public String messageAbout3(LocalizedAccessor.Message3 message3, String arg3) {
    return message3.formatMessage(myAuthorName, DateUtil.toLocalDateOrTime(myCreated), arg3);
  }
}
