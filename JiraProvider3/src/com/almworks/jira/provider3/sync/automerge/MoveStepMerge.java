package com.almworks.jira.provider3.sync.automerge;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.items.sync.*;
import com.almworks.jira.provider3.remotedata.issue.MoveIssueStep;

class MoveStepMerge implements ItemAutoMerge {
  @Override
  public void preProcess(ModifiableDiff local) {
    long moveKind = getMoveKind(local);
    if (moveKind <= 0) return;
    if (local.getElderVersion().isInvisible())
      for (HistoryRecord record : local.getHistory()) {
        if (moveKind != record.getKind())
          continue;
        local.removeHistoryRecord(record.getRecordId());
      }
  }

  private long getMoveKind(ItemDiff local) {
    return MoveIssueStep.STEP_KIND.findItem(local.getReader());
  }

  @Override
  public void resolve(AutoMergeData data) {
    long moveKind = getMoveKind(data.getLocal());
    if (moveKind <= 0) return;
    IntArray moveRecords = new IntArray();
    for (HistoryRecord record : data.getHistory()) {
      if (moveKind != record.getKind()) continue;
      MoveIssueStep step = MoveIssueStep.load(data.getReader(), record.getDataStream());
      if (step == null) continue;
      moveRecords.add(record.getRecordId());
      if (step.sameTarget(data.getServer().getNewerVersion())) {
        for (IntIterator cursor : moveRecords) data.removeHistoryRecord(cursor.value());
        moveRecords.clear();
      }
    }
  }
}
