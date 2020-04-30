package com.almworks.timetrack.impl;

import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.util.Pair;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.Persistable;
import com.almworks.util.io.persist.PersistableArrayList;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableInteger;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimeTrackerState {
  private static final String TIME_DATA_KEY = "timeData";
  private static final String REMAINING_TIMES_KEY = "remainingTimes";
  private static final String SPENT_DELTAS_KEY = "spentDeltas";
  private static final String CURRENT_TASKS_KEY = "currentTasks";
  private static final String TRACKING_KEY = "tracking";
  private static final String LAST_TASK_KEY = "lastTask";

  /**
   * Map from task to list of timings, sorted by getStarted()
   */
  private final Map<TimeTrackerTask, List<TaskTiming>> myTimeData = Collections15.hashMap();
  private final Map<TimeTrackerTask, TaskRemainingTime> myRemainingTimes = Collections15.hashMap();
  private final Map<TimeTrackerTask, Integer> mySpentDeltas = Collections15.hashMap();
  
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private TimeTrackerTask myCurrentTask;
  private TimeTrackerTask myLastTask;
  private boolean myTracking;

  private final Persistable<Map<TimeTrackerTask, List<TaskTiming>>> myTimeDataPersister =
    PersistableHashMap.create(new TimeTrackerTask.Persister(), PersistableArrayList.create(new TaskTiming.Persister()));

  private final Persistable<Map<TimeTrackerTask, TaskRemainingTime>> myRemainingTimesPersister =
    PersistableHashMap.create(new TimeTrackerTask.Persister(), new TaskRemainingTime.Persister());

  private final Persistable<Map<TimeTrackerTask, Integer>> mySpentDeltasPersister =
    PersistableHashMap.create(new TimeTrackerTask.Persister(), new PersistableInteger());

  private final Persistable<List<TimeTrackerTask>> myCurrentTasksPersister =
    PersistableArrayList.create(new TimeTrackerTask.Persister());

  private final Persistable<List<TimeTrackerTask>> myLastTasksPersister =
    PersistableArrayList.create(new TimeTrackerTask.Persister());

  private AutoPauseReason myLastAutoPauseReason;
  private long myLastAutoPauseTime;
  private static final String LAST_AUTO_PAUSE_REASON_KEY = "lastAutoPauseReason";
  private static final String LAST_AUTO_PAUSE_TIME_KEY = "lastAutoPauseTime";

  private boolean checkInvariants() {
    final List<TaskTiming> allTimings = Collections15.arrayList();
    boolean success = true;

    // check order
    for (Map.Entry<TimeTrackerTask, List<TaskTiming>> e : myTimeData.entrySet()) {
      assert e.getKey() != null : e;
      assert e.getValue() != null : e;

      TaskTiming last = null;
      for (TaskTiming timing : e.getValue()) {
        if (last != null) {
          if (last.getStopped() <= 0 || last.getStopped() > timing.getStarted()) {
            success = false;
            Log.warn("1: " + last + " => " + timing);
          }
        }
        if (timing.getStopped() > 0 && timing.getStarted() > timing.getStopped()) {
          success = false;
          Log.warn("2: " + timing + " (" + timing.getStarted() + ", " + timing.getStopped() + ")");
        }
        last = timing;
      }

      allTimings.addAll(e.getValue());

      // check that current task has open end
      if ((e.getKey().equals(myCurrentTask) && myTracking) != (last != null && last.getStopped() <= 0)) {
        Log.warn("3: " + myCurrentTask + " : " + e.getValue());
        success = false;
      }
      // empty timing lists are not allowed
      if (last == null) {
        Log.warn("4: " + e.getKey());
        success = false;
      }
    }

    // check global order (not just within-task order)
    Collections.sort(allTimings);
    TaskTiming last = null;
    for(final TaskTiming timing : allTimings) {
      if (last != null) {
        if (last.getStopped() <= 0 || last.getStopped() > timing.getStarted()) {
          success = false;
          Log.warn("5: " + last + " => " + timing);
        }
      }
      last = timing;
    }

    return success;
  }

  public void loadFrom(Store store) {
    Map<TimeTrackerTask, List<TaskTiming>> timeData = null;
    Map<TimeTrackerTask, TaskRemainingTime> remTimes = null;
    Map<TimeTrackerTask, Integer> deltas = null;
    List<TimeTrackerTask> currentTask = null;
    List<TimeTrackerTask> lastTask = null;
    final AutoPauseReason[] lastAutoPauseReason = {null};
    final long[] lastAutoPauseTime = {0};
    boolean tracking = false;
    synchronized (this) {
      try {
        if (StoreUtils.restorePersistable(store, TIME_DATA_KEY, myTimeDataPersister)) {
          timeData = myTimeDataPersister.copy();
          sanitizeTimeData(timeData);
        } else {
          Log.warn("cannot restore time data");
        }
        if (StoreUtils.restorePersistable(store, REMAINING_TIMES_KEY, myRemainingTimesPersister)) {
          remTimes = myRemainingTimesPersister.copy();
        } else {
          Log.warn("cannot restore remaining times");
        }
        if (StoreUtils.restorePersistable(store, SPENT_DELTAS_KEY, mySpentDeltasPersister)) {
          deltas = mySpentDeltasPersister.copy();
        } else {
          Log.warn("cannot restore time spent deltas");
        }
        if (StoreUtils.restorePersistable(store, CURRENT_TASKS_KEY, myCurrentTasksPersister)) {
          currentTask = myCurrentTasksPersister.copy();
        } else {
          Log.warn("cannot restore time tracking current tasks");
        }
        tracking = StoreUtils.restoreBoolean(store, "*", TRACKING_KEY);
        if (StoreUtils.restorePersistable(store, LAST_TASK_KEY, myLastTasksPersister)) {
          lastTask = myLastTasksPersister.copy();
        } else {
          Log.warn("cannot restore time tracking last task");
        }
        long lar = StoreUtils.restoreLong(store, "*", LAST_AUTO_PAUSE_REASON_KEY, -1);
        if (lar >= 0) {
          switch ((int)lar) {
          case 0: lastAutoPauseReason[0] = AutoPauseReason.APPLICATION_INACTIVE; break;
          case 1: lastAutoPauseReason[0] = AutoPauseReason.COMPUTER_INACTIVE; break;
          case 2: lastAutoPauseReason[0] = AutoPauseReason.USER_INACTIVE; break;
          }
        }
        long t = StoreUtils.restoreLong(store, "*", LAST_AUTO_PAUSE_TIME_KEY, 0);
        if (t > Const.DAY && t < System.currentTimeMillis()) {
          lastAutoPauseTime[0] = t;
        }
      } finally {
        myTimeDataPersister.clear();
        myRemainingTimesPersister.clear();
        mySpentDeltasPersister.clear();
        myCurrentTasksPersister.clear();
        myLastTasksPersister.clear();
      }
    }
    final Map<TimeTrackerTask, List<TaskTiming>> finalTimeData = timeData;
    final Map<TimeTrackerTask, TaskRemainingTime> finalRemTimes = remTimes;
    final Map<TimeTrackerTask, Integer> finalDeltas = deltas;
    final List<TimeTrackerTask> finalCurrentTask = currentTask;
    final boolean finalTracking = tracking;
    final List<TimeTrackerTask> finalLastTask = lastTask;
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        if (finalTimeData != null) {
          myTimeData.clear();
          myTimeData.putAll(finalTimeData);
        }
        if(finalRemTimes != null) {
          myRemainingTimes.clear();
          myRemainingTimes.putAll(finalRemTimes);
        }
        if(finalDeltas != null) {
          mySpentDeltas.clear();
          mySpentDeltas.putAll(finalDeltas);
        }
        if (finalCurrentTask != null) {
          myCurrentTask = finalCurrentTask.isEmpty() ? null : finalCurrentTask.get(0);
        }
        if (finalLastTask != null) {
          myLastTask = finalLastTask.isEmpty() ? null : finalLastTask.get(0);
        }
        setTrackingNoUpdate(finalTracking);
        myLastAutoPauseReason = lastAutoPauseReason[0];
        myLastAutoPauseTime = lastAutoPauseTime[0];
        fireChanged();
      }
    });
  }

  private static void sanitizeTimeData(Map<TimeTrackerTask, List<TaskTiming>> timeData) {
    final long[] lastStarted = { 0L };

    checkTimings(timeData, new Condition<TaskTiming>() {
      @Override
      public boolean isAccepted(TaskTiming value) {
        if(value.getStarted() <= 0L) {
          return false;
        }
        lastStarted[0] = Math.max(lastStarted[0], value.getStarted());
        return true;
      }
    });

    checkTimings(timeData, new Condition<TaskTiming>() {
      @Override
      public boolean isAccepted(TaskTiming value) {
        if(value.isCurrent() && value.getStarted() < lastStarted[0]) {
          return false;
        }
        return true;
      }
    });
  }

  private static void checkTimings(Map<TimeTrackerTask, List<TaskTiming>> timeData, Condition<TaskTiming> filter) {
    for(final Iterator<List<TaskTiming>> ei = timeData.values().iterator(); ei.hasNext();) {
      final List<TaskTiming> tts = ei.next();
      for(final Iterator<TaskTiming> tti = tts.iterator(); tti.hasNext();) {
        final TaskTiming tt = tti.next();
        if(!filter.isAccepted(tt)) {
          Log.warn("Broken TT " + tt);
          tti.remove();
        }
      }
      if(tts.isEmpty()) {
        ei.remove();
      }
    }
  }

  public void saveTo(Store store) {
    final Map<TimeTrackerTask, List<TaskTiming>> timeData = Collections15.hashMap();
    final Map<TimeTrackerTask, TaskRemainingTime> remTimes = Collections15.hashMap();
    final Map<TimeTrackerTask, Integer> deltas = Collections15.hashMap();
    final List<TimeTrackerTask> currentTask = Collections15.arrayList();
    final List<TimeTrackerTask> lastTask = Collections15.arrayList();
    final boolean[] tracking = {false};
    final AutoPauseReason[] lastAutoPauseReason = {null};
    final long[] lastAutoPauseTime = {0};

    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        timeData.putAll(myTimeData);
        for (Map.Entry<TimeTrackerTask, List<TaskTiming>> e : timeData.entrySet()) {
          e.setValue(Collections15.arrayList(e.getValue()));
        }
        remTimes.putAll(myRemainingTimes);
        deltas.putAll(mySpentDeltas);
        if(myCurrentTask != null) {
          currentTask.add(myCurrentTask);
        }
        tracking[0] = myTracking;
        if(myLastTask != null) {
          lastTask.add(myLastTask);
        }
        lastAutoPauseReason[0] = myLastAutoPauseReason;
        lastAutoPauseTime[0] = myLastAutoPauseTime;
      }
    });

    synchronized (this) {
      try {
        myTimeDataPersister.set(timeData);
        myRemainingTimesPersister.set(remTimes);
        mySpentDeltasPersister.set(deltas);
        myCurrentTasksPersister.set(currentTask);
        myLastTasksPersister.set(lastTask);
        StoreUtils.storePersistable(store, TIME_DATA_KEY, myTimeDataPersister);
        StoreUtils.storePersistable(store, REMAINING_TIMES_KEY, myRemainingTimesPersister);
        StoreUtils.storePersistable(store, SPENT_DELTAS_KEY, mySpentDeltasPersister);
        StoreUtils.storePersistable(store, CURRENT_TASKS_KEY, myCurrentTasksPersister);
        StoreUtils.storePersistable(store, LAST_TASK_KEY, myLastTasksPersister);
        StoreUtils.storeBoolean(store, "*", TRACKING_KEY, tracking[0]);
        int lar = -1;
        if (lastAutoPauseReason[0] == AutoPauseReason.APPLICATION_INACTIVE)
          lar = 0;
        else if (lastAutoPauseReason[0] == AutoPauseReason.COMPUTER_INACTIVE)
          lar = 1;
        else if (lastAutoPauseReason[0] == AutoPauseReason.USER_INACTIVE)
          lar = 2;
        StoreUtils.storeLong(store, "*", LAST_AUTO_PAUSE_REASON_KEY, lar);
        StoreUtils.storeLong(store, "*", LAST_AUTO_PAUSE_TIME_KEY, lastAutoPauseTime[0]);
      } finally {
        myTimeDataPersister.clear();
        myRemainingTimesPersister.clear();
        mySpentDeltasPersister.clear();
        myCurrentTasksPersister.clear();
        myLastTasksPersister.clear();
      }
    }
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean isTracking() {
    return myTracking;
  }

  public boolean isEmpty() {
    for(final List<TaskTiming> timings : myTimeData.values()) {
      if(timings != null && !timings.isEmpty()) {
        return false;
      }
    }

    if(!myRemainingTimes.isEmpty()) {
      return false;
    }

    if(!mySpentDeltas.isEmpty()) {
      return false;
    }

    return true;
  }

  public Map<TimeTrackerTask, List<TaskTiming>> getRecordedTimings(boolean stopCurrentNow) {
    long now = now();
    LinkedHashMap<TimeTrackerTask, List<TaskTiming>> r = Collections15.linkedHashMap(myTimeData);
    for (Map.Entry<TimeTrackerTask, List<TaskTiming>> e : r.entrySet()) {
      List<TaskTiming> timings = e.getValue();
      List<TaskTiming> replacement = Collections15.arrayList();
      for (TaskTiming timing : timings) {
        final boolean current = timing.isCurrent();
        replacement.add(new TaskTiming(
          timing.getStarted(),
          (stopCurrentNow && current) ? now : timing.getStopped(),
          timing.getComments(),
          current));
      }
      e.setValue(replacement);
    }
    return r;
  }

  public List<TaskTiming> getTaskTimings(TimeTrackerTask task) {
    Threads.assertAWTThread();
    List<TaskTiming> list = myTimeData.get(task);
    return list == null ? null : Collections.unmodifiableList(list);
  }

  public TimeTrackerTask getLastTask() {
    return myLastTask;
  }

  public boolean removeTimingsNoUpdate(long from, long to) {
    if (from >= to)
      return false;
    boolean changed = false;
    for (Iterator<List<TaskTiming>> ii = myTimeData.values().iterator(); ii.hasNext();) {
      List<TaskTiming> timings = ii.next();
      changed |= removeTimingsFromList(from, to, timings);
      if (timings.isEmpty()) {
        ii.remove();
      }
    }
    return changed;
  }

  private boolean removeTimingsFromList(long from, long to, List<TaskTiming> timings) {
    boolean changed = false;
    TaskTiming seek = new TaskTiming(from, 0, "");
    int k = Collections.binarySearch(timings, seek);
    if (k < 0)
      k = -k - 1;
    if (k > 0)
      k--;
    while (k < timings.size()) {
      TaskTiming t = timings.get(k);
      if (t.getStarted() >= to)
        break;
      if (t.getStopped() > from || t.getStopped() <= 0) {
        changed = true;
        timings.remove(k);
        if (t.getStarted() < from - TimeTrackingUtil.MINIMAL_INTERVAL) {
          timings.add(k, new TaskTiming(t.getStarted(), from, t.getComments()));
          k++;
        }
        if (t.getStopped() > to + TimeTrackingUtil.MINIMAL_INTERVAL || t.getStopped() <= 0) {
          timings.add(k, new TaskTiming(to, t.getStopped(), t.getComments()));
          k++;
        }
      } else {
        k++;
      }
    }
    return changed;
  }

  public boolean removeTimingsNoUpdate(TimeTrackerTask task, long from, long to) {
    Threads.assertAWTThread();
    List<TaskTiming> timings = myTimeData.get(task);
    if (timings == null) return false;
    boolean changed = removeTimingsFromList(from, to, timings);
    if (!changed) return false;
    if (timings.isEmpty()) {
      myTimeData.remove(task);
    }
    fireChanged();
    return true;
  }

  public void setTrackingAndCurrentTask(boolean tracking, TimeTrackerTask task) {
    boolean c1 = setTrackingNoUpdate(tracking);
    boolean c2 = setCurrentTaskNoUpdate(task);
    if (c1 || c2) {
      fireChanged();
    }
  }

  public Pair<TimeTrackerTask, TaskTiming> getPrecedingTiming(long time) {
    // dumb
    TimeTrackerTask bestTask = null;
    TaskTiming bestTiming = null;
    for (Map.Entry<TimeTrackerTask, List<TaskTiming>> e : myTimeData.entrySet()) {
      TaskTiming t = null;
      for (TaskTiming timing : e.getValue()) {
        if (timing.getStarted() >= time || timing.getStopped() > time || timing.getStopped() <= 0)
          break;
        t = timing;
      }
      if (t != null) {
        assert t.getStopped() <= time;
        if (bestTiming == null || bestTiming.getStopped() < t.getStopped()) {
          bestTask = e.getKey();
          bestTiming = t;
        }
      }
    }
    return bestTiming == null ? null : Pair.create(bestTask, bestTiming);
  }

  public boolean replaceTiming(TimeTrackerTask task, TaskTiming timing, TaskTiming newTiming) {
    Threads.assertAWTThread();
    List<TaskTiming> data = myTimeData.get(task);
    if (data == null)
      return false;
    int i = data.indexOf(timing);
    if (i < 0)
      return false;

    data.remove(i);

    if (newTiming == null ||
      (newTiming.getStarted() + TimeTrackingUtil.MINIMAL_INTERVAL >= newTiming.getStopped() && newTiming.getStopped() > 0 &&
        !data.isEmpty()))
    {
      // just remove, no adjustments
      if (timing.getStopped() <= 0) {
        if (Util.equals(myCurrentTask, task)) {
          myCurrentTask = null;
          setTrackingNoUpdate(false);
        }
      }
    } else {
      long from = newTiming.getStarted();
      long to = newTiming.getStopped();
      if (to <= 0) {
        // current work
        if (!task.equals(getCurrentTask())) {
          setCurrentTaskNoUpdate(task);
        }
        to = now();
      }
      removeTimingsNoUpdate(from, to);

      // need to reget after changes done be remove
      data = getOrCreateTimings(task);
      int k = Collections.binarySearch(data, newTiming);
      if (k < 0)
        k = -k - 1;
      data.add(k, newTiming);
    }

    if(data.isEmpty()) {
      myTimeData.remove(task);
    }

    fireChanged();
    return true;
  }

  private void fireChanged() {
    assert checkInvariants();
    myModifiable.fireChanged();
  }

  public void setTracking(boolean tracking) {
    boolean changed = setTrackingNoUpdate(tracking);
    if (changed) {
      fireChanged();
    }
  }

  private boolean setTrackingNoUpdate(boolean tracking) {
    Threads.assertAWTThread();
    boolean changed = myTracking != tracking;
    if (changed) {
      myTracking = tracking;
      if (!tracking) {
        myLastAutoPauseReason = null;
        myLastAutoPauseTime = 0;
      }
      long now = now();
      if (myCurrentTask != null) {
        if (tracking) {
          startTask(myCurrentTask, now);
        } else {
          stopTask(myCurrentTask, now);
        }
      }
    }
    return changed;
  }

  @Nullable
  public TimeTrackerTask getCurrentTask() {
    Threads.assertAWTThread();
    return myCurrentTask;
  }

  private boolean setCurrentTaskNoUpdate(TimeTrackerTask task) {
    Threads.assertAWTThread();
    long now = now();
    TimeTrackerTask old = myCurrentTask;
    boolean changed = false;
    if (task != null) {
      changed = !Util.equals(task, myLastTask);
      myLastTask = task;
    }
    if (!Util.equals(task, old)) {
      changed = true;
      myCurrentTask = task;
      myLastAutoPauseReason = null;
      myLastAutoPauseTime = 0;
      if (myTracking) {
        if (old != null) {
          stopTask(old, now);
        }
        if (task != null) {
          startTask(task, now);
        }
      }
    }
    return changed;
  }

  private void stopTask(TimeTrackerTask task, long now) {
    Threads.assertAWTThread();
    List<TaskTiming> timings = getOrCreateTimings(task);
    int index = timings.size() - 1;
    if (index < 0) {
      assert false : task;
      return;
    }
    TaskTiming t = timings.get(index);
    if (t.getStarted() > now || t.getStopped() > 0) {
      assert false : t;
      return;
    }
    if (now - t.getStarted() < TimeTrackingUtil.MINIMAL_INTERVAL) {
      timings.remove(index);
      if(timings.isEmpty()) {
        myTimeData.remove(task);
      }
    } else {
      TaskTiming stopped = new TaskTiming(t.getStarted(), now, t.getComments());
      timings.set(index, stopped);
    }
  }

  private void startTask(TimeTrackerTask task, long now) {
    Threads.assertAWTThread();
    TaskTiming t = new TaskTiming(now, 0, "");
    List<TaskTiming> timings = getOrCreateTimings(task);
    // clear all events that happenned later
    int i = Collections.binarySearch(timings, t);
    if (i < 0)
      i = -i - 1;
    if (i < timings.size()) {
      timings.subList(i, timings.size()).clear();
    }
    if (i > 0) {
      TaskTiming prev = timings.get(i - 1);
      long stopped = prev.getStopped();
      if (stopped <= 0) {
        // already tracking
        return;
      } else if (stopped >= now - TimeTrackingUtil.MINIMAL_INTERVAL) {
        // replace
        timings.remove(i - 1);
        t = new TaskTiming(prev.getStarted(), 0, prev.getComments());
      }
    }
    timings.add(t);
  }

  private List<TaskTiming> getOrCreateTimings(TimeTrackerTask task) {
    Threads.assertAWTThread();
    List<TaskTiming> r = myTimeData.get(task);
    if (r == null) {
      r = Collections15.arrayList();
      myTimeData.put(task, r);
    }
    return r;
  }

  protected long now() {
    return System.currentTimeMillis();
  }

  public void clear() {
    Threads.assertAWTThread();
    myCurrentTask = null;
    myLastTask = null;
    myTimeData.clear();
    myRemainingTimes.clear();
    mySpentDeltas.clear();
    myTracking = false;
    myLastAutoPauseReason = null;
    myLastAutoPauseTime = 0;
  }

  public TaskTiming getLastTiming(TimeTrackerTask task) {
    List<TaskTiming> list = myTimeData.get(task);
    return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
  }

  /**
   * Stops tracking and adjusts last work period to stop earlier
   */
  public void setAutoPaused(long workStopTime, AutoPauseReason reason, long falseResumeTimeout) {
    Threads.assertAWTThread();
    if (!myTracking) {
      assert false : this;
      return;
    }
    TimeTrackerTask task = getCurrentTask();
    if (task == null) {
      assert false : this;
      return;
    }
    stopTask(task, workStopTime);
    if (myLastAutoPauseReason != null && falseResumeTimeout > 0) {
      // check that the last work period isn't too small
      List<TaskTiming> list = myTimeData.get(task);
      if(list != null && !list.isEmpty()) {
        final int lastIndex = list.size() - 1;
        final TaskTiming timing = list.get(lastIndex);
        if(lastIndex > 0 && timing != null && timing.getStopped() > 0 &&
          (timing.getStopped() - timing.getStarted()) < falseResumeTimeout)
        {
          list.remove(lastIndex);
          // ignore false resume timeout
        }
      }
    }
    myTracking = false;
    myLastAutoPauseReason = reason;
    myLastAutoPauseTime = workStopTime;
    fireChanged();
  }

  public void resumeFromAutoPause(long workStartTime) {
    Threads.assertAWTThread();
    if (myTracking) {
      assert false : this;
      return;
    }
    TimeTrackerTask task = getCurrentTask();
    if (task == null) {
      assert false : this;
      return;
    }
    startTask(task, workStartTime);
    myTracking = true;
    fireChanged();
  }

  public AutoPauseReason getLastAutoPauseReason() {
    return myLastAutoPauseReason;
  }

  public long getLastAutoPauseTime() {
    return myLastAutoPauseTime;
  }

  public void addTiming(TimeTrackerTask task, TaskTiming timing) {
    Threads.assertAWTThread();
    if (task == null || timing == null) return;
    if (timing.getStarted() >= timing.getStopped()) return;
    removeTimingsNoUpdate(timing.getStarted(), timing.getStopped());
    List<TaskTiming> data = getOrCreateTimings(task);
    int k = Collections.binarySearch(data, timing);
    if (k < 0)
      k = -k - 1;
    data.add(k, timing);
    fireChanged();
  }

  public void setRemainingTime(TimeTrackerTask task, TaskRemainingTime estimate) {
    Threads.assertAWTThread();
    if(estimate != null) {
      myRemainingTimes.put(task, estimate);
    } else {
      myRemainingTimes.remove(task);
    }
    fireChanged();
  }

  public TaskRemainingTime getRemainingTime(TimeTrackerTask task) {
    Threads.assertAWTThread();
    return myRemainingTimes.get(task);
  }

  public Map<TimeTrackerTask, TaskRemainingTime> getRemainingTimes() {
    Threads.assertAWTThread();
    return Collections.unmodifiableMap(myRemainingTimes);
  }

  public Map<TimeTrackerTask, Integer> getSpentDeltas() {
    Threads.assertAWTThread();
    return Collections.unmodifiableMap(mySpentDeltas);
  }

  public boolean hasUnpublished(long artifactKey) {
    Threads.assertAWTThread();
    for (Map.Entry<TimeTrackerTask,List<TaskTiming>> entry : myTimeData.entrySet())
      if (entry.getKey().getKey() == artifactKey && !entry.getValue().isEmpty()) return true;
    for (TimeTrackerTask task : myRemainingTimes.keySet())
      if (task.getKey() == artifactKey) return true;
    for (TimeTrackerTask task : mySpentDeltas.keySet())
      if (task.getKey() == artifactKey) return true;
    return false;
  }

  public void setSpentDelta(TimeTrackerTask task, Integer delta) {
    Threads.assertAWTThread();
    if(delta != null) {
      mySpentDeltas.put(task, delta);
    } else {
      mySpentDeltas.remove(task);
    }
    fireChanged();
  }

  public void clearStateForItem(long item) {
    Threads.assertAWTThread();
    boolean changed = false;

    if(myCurrentTask != null && myCurrentTask.getKey() == item) {
      setTrackingNoUpdate(false);
      setCurrentTaskNoUpdate(null);
      changed = true;
    }

    if(myLastTask != null && myLastTask.getKey() == item) {
      myLastTask = null;
      changed = true;
    }

    changed |= clearMap(item, myTimeData);
    changed |= clearMap(item, myRemainingTimes);
    changed |= clearMap(item, mySpentDeltas);

    if(changed) {
      fireChanged();
    }
  }

  private <X> boolean clearMap(long item, Map<TimeTrackerTask, X> map) {
    boolean changed = false;
    for(final Iterator<Map.Entry<TimeTrackerTask, X>> it = map.entrySet().iterator(); it.hasNext();) {
      if(it.next().getKey().getKey() == item) {
        it.remove();
        changed = true;
      }
    }
    return changed;
  }
}
