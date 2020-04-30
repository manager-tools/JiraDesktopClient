package com.almworks.items.sync.util;

import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemDiff;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MergeHistoryState {
  private List<HistoryRecord> myUpdatedHistory;
  @Nullable
  private final HistoryRecord[] myHistoryDiff;

  public MergeHistoryState(HistoryRecord[] historyDiff, List<HistoryRecord> updatedHistory) {
    myHistoryDiff = historyDiff;
    myUpdatedHistory = updatedHistory;
  }

  public static MergeHistoryState create(HistoryRecord[] history, int commonHistory) {
    commonHistory = Math.max(0, commonHistory);
    commonHistory = Math.min(commonHistory, history != null ? history.length : 0);
    List<HistoryRecord> updatedHistory = null;
    HistoryRecord[] historyDiff;
    if (commonHistory > 0 && history != null) { // copy not common history tail to update
      updatedHistory = Collections15.arrayList();
      updatedHistory.addAll(Arrays.asList(history).subList(commonHistory, history.length));
      historyDiff = null;
    } else historyDiff = history != null && history.length > 0 ? history : null;
    return new MergeHistoryState(historyDiff, updatedHistory);
  }

  public static MergeHistoryState load(ItemDiff local) {
    HistoryRecord[] history = ItemVersionCommonImpl.getHistory(local.getNewerVersion().switchToTrunk());
    if (history.length == 0) history = null;
    return new MergeHistoryState(history, null);
  }

  public boolean hasHistory() {
    return myUpdatedHistory == null ? myHistoryDiff != null : !myUpdatedHistory.isEmpty();
  }

  public List<HistoryRecord> getHistory() {
    if (myUpdatedHistory == null) return myHistoryDiff != null ? Collections.unmodifiableList(Arrays.asList(myHistoryDiff)) : Collections.<HistoryRecord>emptyList();
    else return Collections15.arrayList(myUpdatedHistory);
  }

  public List<HistoryRecord> getUpdatedHistory() {
    return myUpdatedHistory != null ? Collections.unmodifiableList(myUpdatedHistory) : null;
  }

  public void removeHistoryRecord(int recordId) {
    if (myUpdatedHistory == null) myUpdatedHistory = myHistoryDiff != null ? Collections15.arrayList(myHistoryDiff) : Collections15.<HistoryRecord>arrayList();
    for (Iterator<HistoryRecord> it = myUpdatedHistory.iterator(); it.hasNext(); ) {
      HistoryRecord record = it.next();
      if (record.getRecordId() == recordId) {
        it.remove();
        return;
      }
    }
    LogHelper.error("Record ID not found", recordId);
  }
}
