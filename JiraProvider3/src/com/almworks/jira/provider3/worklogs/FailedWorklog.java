package com.almworks.jira.provider3.worklogs;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.jira.provider3.sync.download2.process.util.SlaveUploadFailures;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

class FailedWorklog {
  private final long myItem;
  private final Date myStarted;
  private final int mySeconds;
  private final String myComment;

  public FailedWorklog(long item, Date started, int seconds, String comment) {
    myItem = item;
    myStarted = started;
    mySeconds = seconds;
    myComment = comment;
  }

  public static void findFailedUploads(EntityWriter writer) {
    new SlaveUploadFailures<FailedWorklog>(ServerWorklog.TYPE, ServerWorklog.ISSUE) {
      @Override
      protected long getItem(FailedWorklog failure) {
        return failure.myItem;
      }

      @Override
      protected boolean matches(FailedWorklog failure, EntityHolder slave) {
        return failure.matches(slave);
      }

      @Override
      protected List<FailedWorklog> loadFailures(DBReader reader, long issueItem) {
        LongArray failures = reader.query(DPEquals.create(Worklog.ISSUE, issueItem).and(DPNotNull.create(SyncSchema.UPLOAD_ATTEMPT))).copyItemsSorted();
        if (failures.isEmpty()) return Collections.emptyList();
        ArrayList<FailedWorklog> result = Collections15.arrayList();
        for (ItemVersion worklog : BranchSource.trunk(reader).readItems(failures)) {
          if (worklog.getSyncState() != SyncState.NEW) continue;
          Date started = worklog.getValue(Worklog.STARTED);
          int seconds = worklog.getNNValue(Worklog.TIME_SECONDS, 0);
          String comment = worklog.getNNValue(Worklog.COMMENT, "").trim();
          if (comment.isEmpty()) comment = null;
          if (started == null || seconds <= 0) continue;
          result.add(new FailedWorklog(worklog.getItem(), started, seconds, comment));
        }
        return result;
      }
    }.perform(writer);
  }

  public boolean matches(EntityHolder entity) {
    Date started = entity.getScalarValue(ServerWorklog.START_DATE);
    Integer seconds = entity.getScalarValue(ServerWorklog.TIME_SECONDS);
    if (started == null || seconds == null) return false;
    if (!myStarted.equals(started) || mySeconds != seconds) return false;
    String comment = entity.getScalarValue(ServerWorklog.COMMENT);
    comment = Util.NN(comment).trim();
    if (comment.isEmpty()) comment = null;
    return Util.equals(comment, myComment);
  }
}
