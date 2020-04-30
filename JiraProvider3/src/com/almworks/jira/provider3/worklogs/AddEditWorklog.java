package com.almworks.jira.provider3.worklogs;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.SlaveIds;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AddEditWorklog extends AddEditSlaveUnit<WorklogValues> {
  private static final LocalizedAccessor.Value M_NOT_FOUND_SHORT = PrepareWorklogsUpload.I18N.getFactory("upload.conflict.notFound.short");
  private static final LocalizedAccessor.Message2 M_NOT_FOUND_FULL = PrepareWorklogsUpload.I18N.message2("upload.conflict.notFound.full");
  private static final LocalizedAccessor.Value M_SERVER_ERROR_SHORT = PrepareWorklogsUpload.I18N.getFactory("upload.failure.errorCode.short");
  private static final LocalizedAccessor.MessageInt M_SERVER_ERROR_FULL = PrepareWorklogsUpload.I18N.messageInt("upload.failure.errorCode.full");

  private final WorklogSet mySet;
  @Nullable
  private final SlaveIds myKnownIds;

  public AddEditWorklog(WorklogSet set, long item, WorklogValues base, WorklogValues change, SlaveIds slaveIds, SlaveIds knownIds) {
    super(item, set.getIssue(), base, change, slaveIds);
    mySet = set;
    myKnownIds = knownIds;
  }

  @Override
  protected UploadProblem checkForConflict(@Nullable EntityHolder issue, @NotNull WorklogValues base) {
    EntityHolder worklog = base.find(issue);
    if (worklog == null) return conflict(M_NOT_FOUND_SHORT.create(), base.messageAbout(M_NOT_FOUND_FULL));
    if (!base.checkServer(worklog)) {
      LogHelper.debug("Worklog conflict", base);
      return conflict(PrepareWorklogsUpload.M_CONFLICT_SHORT.create(), base.messageAbout(PrepareWorklogsUpload.M_CONFLICT_FULL));
    }
    return null;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, int issueId, @Nullable EditIssue edit, @Nullable WorklogValues base, WorklogValues change)
    throws ConnectorException
  {
    Integer id = change.getId();
    RestResponse response = id == null ? submitWorklog(session, issueId, change) : editWorklog(session, issueId, change, id);
    if (!response.isSuccessful()) {
      LogHelper.debug("Worklog upload failed", response.getStatusCode(), response.getString());
      return UploadProblem.fatal(M_SERVER_ERROR_SHORT.create(), M_SERVER_ERROR_FULL.formatMessage(response.getStatusCode())).toCollection();
    }
    mySet.markDone(this, true);
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  private RestResponse editWorklog(RestSession session, int issueId, WorklogValues change, int worklogId) throws ConnectorException {
    return session.restPut("api/2/issue/" + issueId + "/worklog/" + worklogId + "?" + mySet.getUpdateEstimate(), change.createJson(), RequestPolicy.NEEDS_LOGIN);
  }

  private static final Pattern NEW_WORKLOG_LOCATION = Pattern.compile("worklog/(\\d+)$");
  private RestResponse submitWorklog(RestSession session, int issueId, WorklogValues change) throws ConnectorException {
    RestResponse response = session.restPostJson("api/2/issue/" + issueId + "/worklog?" + mySet.getUpdateEstimate(), change.createJson(), RequestPolicy.NEEDS_LOGIN);
    if (response.isSuccessful()) processLocation(response, change);
    return response;
  }

  private void processLocation(RestResponse response, WorklogValues change) {
    String location = response.getResponseHeader("Location");
    if (location == null) {
      LogHelper.warning("Missing new worklog location");
      return;
    }
    Matcher m = NEW_WORKLOG_LOCATION.matcher(location);
    if (!m.find()) {
      LogHelper.warning("Wrong new worklog location", location);
      return;
    }
    String idStr = m.group(1);
    try {
      int id = Integer.parseInt(idStr);
      change.setId(id);
      myKnownIds.addNewId(id);
    } catch (NumberFormatException e) {
      LogHelper.warning("Wrong new worklog location id", location, idStr);
    }
  }

  @Override
  protected void doFinishUpload(PostUploadContext context, EntityHolder issue, long item, WorklogValues change, boolean newSlave) {
    EntityHolder worklog;
    Integer id = change.getId();
    if (id == null) {
      if (myKnownIds == null) {
        LogHelper.error("Missing known ids", change);
        return;
      }
      myKnownIds.searchForSubmitted(issue, context, change);
      worklog = change.find(issue);
      if (worklog == null) LogHelper.warning("Worklog submit not confirmed", change);
    } else worklog = change.find(issue);
    if (worklog != null) {
      if (newSlave) worklog.setItem(item);
      else if (!change.checkServer(worklog)) return;
      reportUploaded(context, item);
      mySet.finishUpload(context, issue, item);
    }
  }

  public static void reportUploaded(PostUploadContext context, long item) {
    context.reportUploaded(item, SyncSchema.INVISIBLE);
    context.reportUploaded(item, Worklog.STARTED);
    context.reportUploaded(item, Worklog.TIME_SECONDS);
    context.reportUploaded(item, Worklog.SECURITY);
    context.reportUploaded(item, Worklog.COMMENT);
    context.reportUploaded(item, Worklog.EDITOR);
    context.reportUploaded(item, Worklog.AUTO_ADJUST);
  }
}
