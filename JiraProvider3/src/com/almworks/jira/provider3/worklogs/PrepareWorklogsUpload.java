package com.almworks.jira.provider3.worklogs;

import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.jira.provider3.services.upload.CollectUploadContext;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;

import java.util.Collection;
import java.util.Collections;

public class PrepareWorklogsUpload implements UploadUnit.Factory {
  public static final PrepareWorklogsUpload INSTANCE = new PrepareWorklogsUpload();
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(PrepareWorklogsUpload.class.getClassLoader(), "com/almworks/jira/provider3/worklogs/message");
  static final LocalizedAccessor.Value M_CONFLICT_SHORT = PrepareWorklogsUpload.I18N.getFactory("upload.conflict.conflict.short");
  static final LocalizedAccessor.Message2 M_CONFLICT_FULL = PrepareWorklogsUpload.I18N.message2("upload.conflict.conflict.full");

  @Override
  public void collectRelated(ItemVersion trunk, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ItemVersion issue = trunk.readValue(Worklog.ISSUE);
    if (issue != null && issue.getValue(Issue.ID) == null) context.requestUpload(issue.getItem(), true);
  }

  @Override
  public Collection<? extends UploadUnit> prepare(ItemVersion worklog, LoadUploadContext context) throws UploadUnit.CantUploadException {
    CreateIssueUnit issue = CreateIssueUnit.getExisting(worklog.readValue(Worklog.ISSUE), context);
    if (issue == null) return null;
    SyncState state = worklog.getSyncState();
    UploadUnit unit;
    switch (state) {
    case NEW:
      unit = WorklogSet.submit(context, worklog, issue);
      break;
    case EDITED:
      unit = WorklogSet.edit(context, worklog, issue);
      break;
    case LOCAL_DELETE:
      unit = WorklogSet.delete(context, worklog, issue);
      break;
    case SYNC:
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT:
    default:
      throw UploadUnit.CantUploadException.create("Not uploadable comment state", state, worklog);
    }
    return Collections.singleton(unit);
  }

  public static IssueFieldValue createEditValue(LoadUploadContext context, CreateIssueUnit create, ItemVersion issue) {
    return WorklogSet.getInstance(context, create, issue);
  }

  static UploadProblem createConflict(long item, WorklogValues base) {
    return UploadProblem.conflict(item, PrepareWorklogsUpload.M_CONFLICT_SHORT.create(), base.messageAbout(PrepareWorklogsUpload.M_CONFLICT_FULL));
  }

  public static void findFailedUploads(EntityWriter writer) {
    FailedWorklog.findFailedUploads(writer);
  }
}
