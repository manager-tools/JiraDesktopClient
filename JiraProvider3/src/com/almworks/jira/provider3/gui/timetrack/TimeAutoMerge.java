package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.items.sync.*;
import com.almworks.items.sync.util.merge.AutoMergeConflictGroup;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Iterator;
import java.util.List;

/**
 * Merges timetracking attributes.<br>
 * 1. {@link Issue#TIME_SPENT} and {@link Issue#LOCAL_WORKLOGS} are always merged because of they never can be edited
 * directly by user.<br>
 * 2. {@link Issue#ORIGINAL_ESTIMATE} is always discarded to server value since it can be edited only until any work is logged,
 * so actually cannot be directly edited at all, it is controlled by JIRA server.<br>
 * Other attributes are merge depends on time tracking mode (auto adjustment or manual set). In auto adjustment mode any merge
 * is possible only if all local worklogs changes are merged (not conflict) or can be merged a bit later (server version is
 * equal to local)<br>
 * 3. {@link Issue#LOCAL_REMAIN_ESTIMATE} is always merged. It can be null (in case no auto adjustment) and equals to
 * server value in the case. Or it adjusted (in case of not null) to reflect server side changes of remain estimate.<br>
 * 4. {@link Issue#REMAIN_ESTIMATE} is always merged in case of autoadjustment - since actually user has not changed it directly,
 * it reflects auto calculated sum. The attribute can be left in conflict in case it is concurrently edited locally and on server.
 */
public class TimeAutoMerge implements ItemAutoMerge {
  private static final AutoMergeConflictGroup TIME_TRACK_GROUP =
    new AutoMergeConflictGroup(Issue.TIME_SPENT, Issue.ORIGINAL_ESTIMATE, Issue.REMAIN_ESTIMATE,
      Issue.LOCAL_REMAIN_ESTIMATE, Issue.LOCAL_WORKLOGS);

  @Override
  public void preProcess(ModifiableDiff local) {
    TIME_TRACK_GROUP.preProcess(local);
  }

  @Override
  public void resolve(AutoMergeData data) {
    if (!TIME_TRACK_GROUP.hasAny(data.getUnresolved()) && !TIME_TRACK_GROUP.hasAny(data.getLocal().getChanged())) return;
    List<ItemVersion> worklogs = getLocalWorklogs(data);
    if (worklogs.isEmpty()) data.discardEdit(Issue.TIME_SPENT, Issue.LOCAL_WORKLOGS);
    else data.resolveToLocal(Issue.TIME_SPENT, Issue.LOCAL_WORKLOGS);
    if (data.getLocal().getNewerValue(Issue.LOCAL_REMAIN_ESTIMATE) != null) mergeAutoAdjust(data, worklogs);
    else {
      data.setResolution(Issue.LOCAL_REMAIN_ESTIMATE, null);
      boolean server = data.getServer().isChanged(Issue.REMAIN_ESTIMATE);
      boolean local = data.getLocal().isChanged(Issue.REMAIN_ESTIMATE);
      if (!server || !local) {
        data.discardEdit(Issue.ORIGINAL_ESTIMATE);
        if (server) data.discardEdit(Issue.REMAIN_ESTIMATE);
        else data.resolveToLocal(Issue.REMAIN_ESTIMATE);
      } else if (Util.equals(data.getLocal().getNewerValue(Issue.REMAIN_ESTIMATE), data.getServer().getNewerValue(Issue.REMAIN_ESTIMATE))) {
        data.discardEdit(Issue.ORIGINAL_ESTIMATE);
        data.discardEdit(Issue.REMAIN_ESTIMATE);
      }
    }
  }

  private void mergeAutoAdjust(AutoMergeData data, List<ItemVersion> localWorklogs) {
    if (localWorklogs.isEmpty()) {
      data.setResolution(Issue.LOCAL_REMAIN_ESTIMATE, null);
      data.setResolution(Issue.LOCAL_WORKLOGS, null);
      data.discardEdit(Issue.ORIGINAL_ESTIMATE, Issue.REMAIN_ESTIMATE);
      return;
    }
    for (ItemVersion worklog : localWorklogs) {
      if (!Worklog.isMergeable(data.getReader(), worklog.getItem())) return;
    }
    int newRemain = data.getServer().getNewerNNVale(Issue.REMAIN_ESTIMATE, 0);
    int oldRemain = data.getServer().getElderNNValue(Issue.REMAIN_ESTIMATE, 0);
    int diff = newRemain - oldRemain;
    if (diff != 0) {
      Integer localBase = data.getLocal().getNewerValue(Issue.LOCAL_REMAIN_ESTIMATE);
      if (localBase != null) data.setResolution(Issue.LOCAL_REMAIN_ESTIMATE, Math.max(localBase + diff, 0));
      else LogHelper.error("Wrong merge happens", data.getLocal().getNewerVersion());
    }
    data.discardEdit(Issue.ORIGINAL_ESTIMATE);
    data.resolveToLocal(Issue.REMAIN_ESTIMATE);
  }

  private List<ItemVersion> getLocalWorklogs(AutoMergeData issue) {
    ItemVersion issueVersion = issue.getLocal().getNewerVersion();
    List<ItemVersion> worklogs = Collections15.arrayList(issueVersion.readItems(issueVersion.getSlaves(Worklog.ISSUE)));
    for (Iterator<ItemVersion> it = worklogs.iterator(); it.hasNext(); ) {
      ItemVersion worklog = it.next();
      if (worklog.getSyncState() == SyncState.SYNC) it.remove();
    }
    return worklogs;
  }
}
