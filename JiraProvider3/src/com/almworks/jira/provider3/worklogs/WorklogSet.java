package com.almworks.jira.provider3.worklogs;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.remotedata.issue.SlaveIds;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class WorklogSet implements IssueFieldValue {
  private static final TypedKey<WorklogSet> KEY = TypedKey.create("worklogs");
  private LocalizedAccessor.Value M_TIME_CONFLICT = PrepareWorklogsUpload.I18N.getFactory("upload.conflict.issueTime.short");
  private LocalizedAccessor.Value M_DISPLAY_NAME = PrepareWorklogsUpload.I18N.getFactory("upload.issueTime.name");

  private final List<UploadUnit> myToUpload = Collections15.arrayList();
  private final LongSet myChangedLogs;
  private final CreateIssueUnit myIssue;
  private final Integer myExpectedSpent;
  private final Integer myExpectedEstimate;
  private final Integer myNewEstimate;
  private boolean myEstimateDone = false;
  /** Flag indicates that issue's estimate is being uploaded by worklogs */
  private boolean myWithWorklogs = false;

  private WorklogSet(CreateIssueUnit issue, Integer expectedSpent, Integer expectedEstimate, Integer newEstimate, LongSet changedLogs) {
    myChangedLogs = changedLogs;
    myIssue = issue;
    myExpectedSpent = expectedSpent;
    myExpectedEstimate = expectedEstimate;
    myNewEstimate = newEstimate;
    if (Util.equals(expectedEstimate, newEstimate)) myEstimateDone = true;
  }

  public CreateIssueUnit getIssue() {
    return myIssue;
  }

  public static UploadUnit submit(LoadUploadContext context, ItemVersion worklog, CreateIssueUnit issue) throws UploadUnit.CantUploadException {
    WorklogValues change = WorklogValues.load(worklog);
    SlaveIds slaveIds = SlaveIds.markUpload(context, worklog, ServerWorklog.ISSUE, ServerWorklog.TYPE, ServerWorklog.ID);
    SlaveIds knownIds = SlaveIds.forceLoad(context, worklog, ServerWorklog.ISSUE, ServerWorklog.TYPE, ServerWorklog.ID);
    WorklogSet set = getInstance(context, issue, worklog);
    AddEditWorklog unit = new AddEditWorklog(set, worklog.getItem(), null, change, slaveIds, knownIds);
    set.add(unit);
    return unit;
  }

  public static UploadUnit edit(LoadUploadContext context, ItemVersion worklog, CreateIssueUnit issue) throws UploadUnit.CantUploadException {
    WorklogValues change = WorklogValues.load(worklog);
    WorklogValues base = WorklogValues.load(worklog.switchToServer());
    WorklogSet set = getInstance(context, issue, worklog);
    AddEditWorklog unit = new AddEditWorklog(set, worklog.getItem(), base, change, null, null);
    set.add(unit);
    return unit;
  }

  public static UploadUnit delete(LoadUploadContext context, ItemVersion worklog, CreateIssueUnit issue) throws UploadUnit.CantUploadException {
    WorklogValues base = WorklogValues.load(worklog.switchToServer());
    WorklogSet set = getInstance(context, issue, worklog);
    DeleteWorklog unit = new DeleteWorklog(set, worklog.getItem(), issue, base);
    set.add(unit);
    return unit;
  }

  private void add(UploadUnit unit) {
    myToUpload.add(unit);
    myWithWorklogs = true;
  }

  @NotNull
  static WorklogSet getInstance(LoadUploadContext context, CreateIssueUnit issue, VersionSource db) {
    long issueItem = issue.getIssueItem();
    UserDataHolder issueCache = context.getItemCache(issueItem);
    WorklogSet instance = issueCache.getUserData(KEY);
    if (instance == null) {
      ItemVersion trunk = db.forItem(issueItem).switchToTrunk();
      ItemVersion server = trunk.switchToServer();
      LongSet changedWorklogs = new LongSet();
      for (ItemVersion w : trunk.readItems(trunk.getSlaves(Worklog.ISSUE))) if (w.getSyncState() != SyncState.SYNC) changedWorklogs.add(w.getItem());
      Integer expectedEstimate = server.getValue(Issue.REMAIN_ESTIMATE);
      Integer newEstimate = trunk.getValue(Issue.REMAIN_ESTIMATE);
      if (newEstimate == null && expectedEstimate != null) newEstimate = 0;
      instance = new WorklogSet(issue, server.getValue(Issue.TIME_SPENT), expectedEstimate, newEstimate, changedWorklogs);
      issueCache.putUserData(KEY, instance);
    }
    return instance;
  }

  @Override
  public String checkInitialState(EntityHolder issue) {
    Integer spent = issue.getScalarValue(ServerIssue.TIME_SPENT);
    Integer estimate = issue.getScalarValue(ServerIssue.REMAIN_ESTIMATE);
    if (!Util.equals(myExpectedSpent, spent) || !Util.equals(myExpectedEstimate, estimate)) {
      LogHelper.debug("TimeTracking conflict:", estimate, spent, myExpectedEstimate, myExpectedSpent);
      return M_TIME_CONFLICT.create();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void addChange(EditIssueRequest edit) throws UploadProblem.Thrown {
    if (myWithWorklogs) return;
    String operation = edit.isCreate() ? "set" : "edit";
    if (!needsUpload(edit.getServerInfo()) || !edit.hasOperation(ServerFields.TIME_TRACKING.getJiraId(), operation)) return;
    String newEstimate;
    if (myNewEstimate == null) newEstimate = null;
    else {
      int min = myNewEstimate / 60;
      int hours = min / 60;
      min = min % 60;
      StringBuilder builder = new StringBuilder();
      if (hours > 0) builder.append(hours).append("h");
      if (min > 0) {
        if (builder.length() > 0) builder.append(" ");
        builder.append(min).append("m");
      }
      newEstimate = builder.length() > 0 ? builder.toString() : "0";
    }
    Object minutes = newEstimate;
    edit.addEdit(this, ServerFields.TIME_TRACKING.getJiraId(), UploadJsonUtil.singleObjectElementArray(operation, UploadJsonUtil.object("remainingEstimate", minutes)));
  }

  @Override
  public void uploadDone(boolean success) {
    if (success) myEstimateDone = true;
  }

  @Override
  public boolean needsUpload(RestServerInfo serverInfo) {
    return !myEstimateDone && !myWithWorklogs;
  }

  @Override
  public void finishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
    if (myChangedLogs.isEmpty()) confirmTimeTracking(context, issue);
  }

  @Override
  public String getDisplayName() {
    return M_DISPLAY_NAME.create();
  }

  public void markDone(UploadUnit worklog, boolean uploaded) {
    if (myToUpload.remove(worklog) && uploaded && myToUpload.isEmpty()) myEstimateDone = true;
  }

  public String getUpdateEstimate() {
    if (myToUpload.size() != 1 || myNewEstimate == null) return "adjustEstimate=leave";
    return "adjustEstimate=new&newEstimate=" + (myNewEstimate / 60) + "m";
  }

  public void finishUpload(PostUploadContext context, EntityHolder issue, long worklogItem) {
    if (!myEstimateDone) return;
    myChangedLogs.remove(worklogItem);
    if (!myChangedLogs.isEmpty()) return;
    confirmTimeTracking(context, issue);
  }

  private void confirmTimeTracking(PostUploadContext context, EntityHolder issue) {
    if (myIssue.getEdit() == null) return; // Issue is not during upload - nothing to confirm
    context.reportUploaded(myIssue.getIssueItem(), Issue.LOCAL_WORKLOGS);
    Integer estimate = issue.getScalarValue(ServerIssue.REMAIN_ESTIMATE);
    if (myNewEstimate != null && !myNewEstimate.equals(estimate)) {
      LogHelper.debug("New estimate not upload", myNewEstimate, estimate, myIssue);
      return;
    }
    context.reportUploaded(myIssue.getIssueItem(), Issue.ORIGINAL_ESTIMATE);
    context.reportUploaded(myIssue.getIssueItem(), Issue.REMAIN_ESTIMATE);
    context.reportUploaded(myIssue.getIssueItem(), Issue.TIME_SPENT);
    context.reportUploaded(myIssue.getIssueItem(), Issue.LOCAL_REMAIN_ESTIMATE);
  }
}
