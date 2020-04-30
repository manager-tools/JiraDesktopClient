package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TimeUtils {
  public static LongList selectModified(DBReader reader, LongList worklogs) {
    if (worklogs == null || worklogs.isEmpty()) return LongList.EMPTY;
    LongArray result = new LongArray();
    for (int i = 0; i < worklogs.size(); i++) {
      long item = worklogs.get(i);
      if (reader.getValue(item, SyncSchema.BASE) != null) result.add(item);
    }
    return result;
  }

  public static void isAllOwn(ItemWrapper issue, List<LoadedWorklog> worklogs) throws CantPerformException {
    long user = JiraEditUtils.getIssueConnection(issue).getConnectionUser();
    if (user <= 0) throw new CantPerformException();
    for (LoadedWorklog worklog : worklogs) if (worklog.getWhoItem() != user) throw new CantPerformException();
  }

  public static boolean isAllLocal(List<LoadedWorklog> worklogs) {
    for (LoadedWorklog worklog : worklogs) if (!worklog.getSyncState().isLocalOnly()) return false;
    return true;
  }

  public static void commitAutoAdjust(ItemVersionCreator unsafeIssue, LongList worklogs) {
    Integer baseEstimate = unsafeIssue.getValue(Issue.LOCAL_REMAIN_ESTIMATE);
    if (baseEstimate == null) {
      int localSpent = 0;
      for (ItemVersion worklog : unsafeIssue.readItems(unsafeIssue.getSlaves(Worklog.ISSUE))) {
        if (worklogs.contains(worklog.getItem())) continue;
        localSpent += Worklog.getLocalSpentDelta(worklog.getReader(), worklog.getItem(), true);
      }
      int currentEstimate = Util.NN(unsafeIssue.getValue(Issue.REMAIN_ESTIMATE), 0);
      if (currentEstimate <= 0) baseEstimate = 0;
      else baseEstimate = Math.max(currentEstimate + localSpent, 0);
      unsafeIssue.setValue(Issue.LOCAL_REMAIN_ESTIMATE, baseEstimate);
    }
    for (ItemVersionCreator worklog : unsafeIssue.changeItems(worklogs)) worklog.setValue(Worklog.AUTO_ADJUST, true);
  }

  public static void commitExplicitRemain(ItemVersionCreator issue, Integer estimate, boolean forceNoAuto) {
    Integer localRemainBase = issue.getValue(Issue.LOCAL_REMAIN_ESTIMATE);
    if (localRemainBase == null) {
      TimeUtils.setRemainingEstimate(issue, estimate);
      return;
    }
    if (!forceNoAuto && Util.equals(estimate, localRemainBase)) return;
    TimeUtils.setRemainingEstimate(issue, estimate);
    issue.setValue(Issue.LOCAL_REMAIN_ESTIMATE, null);
  }

  public static void setRemainingEstimate(ItemVersionCreator issue, @Nullable Integer estimate) {
    if (estimate != null && estimate < 0) estimate = null;
    issue.setValue(Issue.REMAIN_ESTIMATE, estimate);
  }

  public static void setRemainingEstimate(DBWriter writer, long issue, @Nullable Integer estimate) {
    if (estimate != null && estimate <= 0) estimate = null;
    writer.setValue(issue, Issue.REMAIN_ESTIMATE, estimate);
  }

  public static boolean equalRemainingEstimate(@Nullable Integer value1, @Nullable Integer value2) {
    int a = Util.NN(value1, 0);
    int b = Util.NN(value2, 0);
    return a == b;
  }

  public static boolean canCreateWorklog(ItemWrapper issue) {
    return issue != null && !issue.services().isRemoteDeleted() && IssuePermissions.hasPermission(issue, IssuePermissions.ADD_WORKLOG);
  }
}
