package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBTrigger;
import com.almworks.items.api.DBWriter;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.AttributeMap;
import com.almworks.itemsync.ApplicationSyncManager;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

class TimeTrigger extends DBTrigger {
  public static final DBTrigger INSTANCE = new TimeTrigger();

  TimeTrigger() {
    super("jira.issueTimes.trigger", DPNotNull.create(Issue.LOCAL_REMAIN_ESTIMATE).or(
      DPNotNull.create(Issue.LOCAL_WORKLOGS)));
  }

  @Override
  public void apply(LongList issues, DBWriter writer) {
    for(final LongIterator it = issues.iterator(); it.hasNext();) adjustTime(writer, it.nextValue());
  }

  private void adjustTime(DBWriter writer, long issue) {
    AttributeMap base = writer.getValue(issue, SyncSchema.BASE);
    if (base == null) {
      writer.setValue(issue, Issue.LOCAL_REMAIN_ESTIMATE, null);
      writer.setValue(issue, Issue.LOCAL_WORKLOGS, null);
      return;
    }
    LongList worklogs = Worklog.collectWorklogs(writer, issue);
    worklogs = TimeUtils.selectModified(writer, worklogs);
    if (worklogs.isEmpty()) {
      writer.setValue(issue, Issue.TIME_SPENT, base.get(Issue.TIME_SPENT));
      Integer localBase = writer.getValue(issue, Issue.LOCAL_REMAIN_ESTIMATE);
      if (localBase != null) {
        TimeUtils.setRemainingEstimate(writer, issue, base.get(Issue.REMAIN_ESTIMATE));
        writer.setValue(issue, Issue.LOCAL_REMAIN_ESTIMATE, null);
      }
      writer.setValue(issue, Issue.LOCAL_WORKLOGS, null);
      if (TimeUtils.equalRemainingEstimate(base.get(Issue.REMAIN_ESTIMATE), writer.getValue(issue, Issue.REMAIN_ESTIMATE))) {
        SyncManager syncManager = ApplicationSyncManager.getInstance(writer);
        if (syncManager != null) syncManager.requestAutoMerge(LongArray.create(issue));
      }
      return;
    }
    updateEstimate(writer, issue, worklogs);
    writer.setValue(issue, Issue.TIME_SPENT, calcTimeSpent(writer, base, worklogs));
  }

  private int calcTimeSpent(DBWriter writer, AttributeMap base, LongList worklogs) {
    int totalSpent = Util.NN(base.get(Issue.TIME_SPENT), 0);
    for (int i = 0; i < worklogs.size(); i++) {
      long worklog = worklogs.get(i);
      int localTime = Worklog.getLocalSpentDelta(writer, worklog, false);
      totalSpent += localTime;
    }
    if (totalSpent < 0) totalSpent = 0;
    return totalSpent;
  }

  private void updateEstimate(DBWriter writer, long issue, LongList worklogs) {
    @Nullable
    Integer estimate = writer.getValue(issue, Issue.LOCAL_REMAIN_ESTIMATE);
    if (estimate == null) return;
    int leftEstimate = estimate;
    for (int i = 0; i < worklogs.size(); i++) {
      long worklog = worklogs.get(i);
      leftEstimate -= Worklog.getLocalSpentDelta(writer, worklog, true);
    }
    if (leftEstimate < 0) leftEstimate = 0;
    TimeUtils.setRemainingEstimate(writer, issue, leftEstimate);
  }
}
