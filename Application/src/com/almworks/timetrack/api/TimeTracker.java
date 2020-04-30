package com.almworks.timetrack.api;

import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Pair;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface TimeTracker {
  Role<TimeTracker> TIME_TRACKER = Role.role("TT", TimeTracker.class);

  Modifiable getModifiable();

  @Nullable
  @ThreadAWT
  TimeTrackerTask getCurrentTask();

  @ThreadAWT
  boolean isCurrentTaskForItem(long item);

  @Nullable
  @ThreadAWT
  TimeTrackerTask getLastTask();

  @ThreadAWT
  void setTracking(boolean tracking);

  @ThreadAWT
  boolean isTracking();

  /**
   * @return true when there are no records
   */
  boolean isEmpty();

  Map<TimeTrackerTask, List<TaskTiming>> getRecordedTimings();

  Map<TimeTrackerTask, List<TaskTiming>> getCurrentTimings();

  List<TaskTiming> getTaskTimings(TimeTrackerTask task);

  void setTrackingAndCurrentTask(boolean tracking, TimeTrackerTask task);

  Pair<TimeTrackerTask, TaskTiming> getPrecedingTiming(long time);

  boolean replaceTiming(TimeTrackerTask task, TaskTiming timing, TaskTiming newTiming);

  boolean isAutoPaused();

  void removePeriod(TimeTrackerTask task, long from, long to);

  void addTiming(TimeTrackerTask task, TaskTiming timing);

  void setRemainingTime(@NotNull TimeTrackerTask task, @Nullable TaskRemainingTime estimate);

  @Nullable
  TaskRemainingTime getRemainingTime(@NotNull TimeTrackerTask task);

  @NotNull
  Map<TimeTrackerTask, TaskRemainingTime> getRemainingTimes();

  void setSpentDelta(@NotNull TimeTrackerTask task, @Nullable Integer delta);

  @NotNull
  Map<TimeTrackerTask, Integer> getSpentDeltas();

  boolean isWindowAlwaysOnTop();

  boolean hasUnpublished(long artifactKey);

  void clearStateForItem(long item);
}
