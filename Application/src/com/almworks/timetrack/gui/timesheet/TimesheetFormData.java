package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.collections.SimpleModifiable;
import org.almworks.util.Collections15;

import java.util.*;

public class TimesheetFormData {
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private final MultiMap<LoadedItem, WorkPeriod> myWorkMap = MultiMap.create();
  private final Map<LoadedItem, TaskRemainingTime> myRemainings = Collections15.hashMap();
  private final Map<LoadedItem, TaskRemainingTime> myAddedRemainings = Collections15.hashMap();
  private final Map<LoadedItem, Integer> mySpentDeltas = Collections15.hashMap();
  private final Map<LoadedItem, Integer> myAddedSpentDeltas = Collections15.hashMap();
  private final Set<LoadedItem> myExcluded = Collections15.hashSet();
  private final OrderListModel<WorkPeriod> myWorkList = OrderListModel.create();
  private final List<WorkPeriod> myAdded = Collections15.arrayList();
  private final List<WorkPeriod> myDeleted = Collections15.arrayList();
  private final Map<WorkPeriod, WorkPeriod> myEdited = Collections15.hashMap();

  public TimesheetFormData(
    Map<LoadedItem, List<TaskTiming>> timeMap,
    Map<LoadedItem, TaskRemainingTime> remainingMap,
    Map<LoadedItem, Integer> timeDeltaMap)
  {
    List<WorkPeriod> periods = new ArrayList<>();
    if (timeMap != null) {
      for (final Map.Entry<LoadedItem, List<TaskTiming>> e : timeMap.entrySet()) {
        final LoadedItem item = e.getKey();

        final List<TaskTiming> timings = e.getValue();
        if (item != null && timings != null) {
          for (TaskTiming timing : timings) {
            if (TimeSheetGrid.okForTiming(timing.getStarted(), timing.getStopped())) {
              WorkPeriod period = new WorkPeriod(timing, item);
              periods.add(period);
              myWorkMap.add(item, period);
            }
          }
        }
      }
    }
    Collections.sort(periods);
    myWorkList.addAll(periods);

    if(remainingMap != null) {
      myRemainings.putAll(remainingMap);
    }

    if(timeDeltaMap != null) {
      mySpentDeltas.putAll(timeDeltaMap);
    }
  }

  public Set<LoadedItem> getArtifacts() {
    return Collections.unmodifiableSet(myWorkMap.keySet());
  }

  public AListModel<WorkPeriod> getWorkList() {
    return myWorkList;
  }

  public Map<LoadedItem, List<TaskTiming>> getTimeMapForPublish() {
    final LinkedHashMap<LoadedItem, List<TaskTiming>> r = Collections15.linkedHashMap();

    for (final WorkPeriod period : myWorkList) {
      final LoadedItem a = period.getArtifact();
      if (period.isExcluded() || myExcluded.contains(a)) {
        continue;
      }

      final TaskTiming timing = period.getTiming();
      if(timing.getLength() < TimeTrackingUtil.MINIMAL_INTERVAL_SEC) {
        continue;
      }

      List<TaskTiming> list = r.get(a);
      if (list == null) {
        list = Collections15.arrayList();
        r.put(a, list);
      }

      list.add(timing);
    }

    return r;
  }

  public void deleteAll(List<WorkPeriod> periods) {
    if (periods == null) {
      return;
    }

    myDeleted.addAll(periods);
    myAdded.removeAll(periods);
    myWorkList.removeAll(periods);
    for(final WorkPeriod p : periods) {
      myWorkMap.remove(p.getArtifact(), p);
      setSpentDeltaNoUpdate(p.getArtifact(), null);
    }

    myModifiable.fireChanged();
  }

  public void addNew(WorkPeriod period) {
    if (period == null) {
      return;
    }

    myAdded.add(period);
    myDeleted.remove(period);
    insertPeriodSorted(period);
    myWorkMap.add(period.getArtifact(), period);
    setSpentDeltaNoUpdate(period.getArtifact(), null);

    myModifiable.fireChanged();
  }

  private void insertPeriodSorted(WorkPeriod period) {
    int k = Collections.binarySearch(myWorkList.toList(), period);
    if (k < 0) {
      k = -k - 1;
    }
    myWorkList.insert(k, period);
  }

  public void replace(WorkPeriod period, WorkPeriod newPeriod) {
    if (newPeriod == null || newPeriod.equals(period)) {
      return;
    }

    if (!myWorkList.remove(period)) {
      return;
    }

    myWorkMap.remove(period.getArtifact(), period);

    final int index = myAdded.indexOf(period);
    if (index >= 0) {
      myAdded.set(index, newPeriod);
    } else {
      myEdited.put(period, newPeriod);
    }

    myDeleted.remove(newPeriod);
    insertPeriodSorted(newPeriod);
    myWorkMap.add(newPeriod.getArtifact(), newPeriod);
    setSpentDeltaNoUpdate(period.getArtifact(), null);
    setSpentDeltaNoUpdate(newPeriod.getArtifact(), null);

    myModifiable.fireChanged();
  }

  public List<WorkPeriod> getDeleted() {
    return myDeleted;
  }

  public List<WorkPeriod> getAdded() {
    return myAdded;
  }

  public Map<WorkPeriod, WorkPeriod> getEdited() {
    return myEdited;
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean hasDataForPublish() {
    for(final WorkPeriod period : myWorkList) {
      if(!period.isExcluded()
        && period.getTiming() != null
        && period.getTiming().getLength() > TimeTrackingUtil.MINIMAL_INTERVAL_SEC)
      {
        return true;
      }
    }

    for(final Map.Entry<LoadedItem, TaskRemainingTime> e : myRemainings.entrySet()) {
      if(e.getValue() != null && !myExcluded.contains(e.getKey())) {
        return true;
      }
    }

    for(final Map.Entry<LoadedItem, Integer> e : mySpentDeltas.entrySet()) {
      if(e.getValue() != null && !myExcluded.contains(e.getKey())) {
        return true;
      }
    }

    return false;
  }

  public boolean hasDataForSave() {
    return !myAdded.isEmpty() || !myDeleted.isEmpty() || !myEdited.isEmpty()
      || !myAddedRemainings.isEmpty() || !myAddedSpentDeltas.isEmpty();
  }

  public void onWorkPeriodChanged() {
    myModifiable.fireChanged();
  }

  public void setRemainingTime(LoadedItem a, TaskRemainingTime rt) {
    if(rt != null) {
      myRemainings.put(a, rt);
    } else {
      myRemainings.remove(a);
    }
    myAddedRemainings.put(a, rt);
    myModifiable.fireChanged();
  }

  public Map<LoadedItem, TaskRemainingTime> getRemainingTimes() {
    return Collections.unmodifiableMap(myRemainings);
  }

  public Map<LoadedItem, TaskRemainingTime> getAddedRemainingTimes() {
    return Collections.unmodifiableMap(myAddedRemainings);
  }

  public Map<LoadedItem, TaskRemainingTime> getRemainingTimesForPublish() {
    final Map<LoadedItem, TaskRemainingTime> result = Collections15.hashMap();
    for(final Map.Entry<LoadedItem, TaskRemainingTime> e : myRemainings.entrySet()) {
      final LoadedItem a = e.getKey();
      if(!myExcluded.contains(a)) {
        result.put(a, e.getValue());
      }
    }
    return result;
  }

  public Map<LoadedItem, List<WorkPeriod>> getWorkMap() {
    return Collections.unmodifiableMap(myWorkMap.toListMap());
  }

  public void setSpentDeltaNoUpdate(LoadedItem item, Integer delta) {
    final boolean changed;
    if(delta != null) {
      mySpentDeltas.put(item, delta);
      changed = true;
    } else {
      changed = mySpentDeltas.remove(item) != null;
    }
    if(changed) {
      myAddedSpentDeltas.put(item, delta);
    }
  }

  public void setSpentDelta(LoadedItem item, Integer delta) {
    setSpentDeltaNoUpdate(item, delta);
    myModifiable.fireChanged();
  }

  public Map<LoadedItem, Integer> getSpentDeltas() {
    return Collections.unmodifiableMap(mySpentDeltas);
  }

  public Map<LoadedItem, Integer> getAddedSpentDeltas() {
    return Collections.unmodifiableMap(myAddedSpentDeltas);
  }

  public Map<LoadedItem, Integer> getSpentDeltasForPublish() {
    final Map<LoadedItem, Integer> result = Collections15.hashMap();
    for(final Map.Entry<LoadedItem, Integer> e : mySpentDeltas.entrySet()) {
      final LoadedItem a = e.getKey();
      if(!myExcluded.contains(a)) {
        result.put(a, e.getValue());
      }
    }
    return result;
  }

  public void setExcluded(LoadedItem item, boolean excluded) {
    if(excluded) {
      myExcluded.add(item);
    } else {
      myExcluded.remove(item);
    }
  }

  public boolean isExcluded(LoadedItem item) {
    return myExcluded.contains(item);
  }
}
