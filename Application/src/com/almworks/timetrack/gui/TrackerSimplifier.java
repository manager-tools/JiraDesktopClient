package com.almworks.timetrack.gui;

import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A tiny class to locally simplify repeated dancing around
 * the {@link TimeTracker} and its complex state.
 */
public class TrackerSimplifier {
  /** The TimeTracker instance. */
  public final TimeTracker tracker;

  /** The flag indicating whether the tracker is tracking or paused/stopped. */
  public final boolean tracking;

  /** The tracker's current task. */
  public final TimeTrackerTask currTask;

  /** The tracker's last task. */
  public final TimeTrackerTask lastTask;

  /** The task we usually care about: either current or last, depending on whether the tracker is tracking or not. */
  public final TimeTrackerTask task;

  public TrackerSimplifier(@NotNull TimeTracker timeTracker) {
    tracker = timeTracker;
    tracking = tracker.isTracking();
    currTask = tracker.getCurrentTask();
    lastTask = tracker.getLastTask();

    assert currTask != null || !tracking;

    task = currTask != null ? currTask : lastTask;
  }

  /**
   * The constructor to initialize the fields
   * from the given {@link ActionContext}
   * @param context The context.
   * @throws CantPerformException If there's no tracker in the context.
   */
  public TrackerSimplifier(@NotNull ActionContext context) throws CantPerformException {
    this(context.getSourceObject(TimeTracker.TIME_TRACKER));
  }

  /**
   * Get the last timing for the given task.
   * @param task The task.
   * @return The last timing or {@code null}.
   */
  @Nullable
  public TaskTiming getLastTiming(TimeTrackerTask task) {
    final List<TaskTiming> timings = tracker.getTaskTimings(task);
    if(timings == null || timings.isEmpty()) {
      return null;
    }
    return timings.get(timings.size() - 1);
  }

  /**
   * Get the last timing for this simplifier's {@link #task}.
   * @return The last timing or {@code null}.
   */
  @Nullable
  public TaskTiming getLastTiming() {
    return getLastTiming(task);
  }

  /**
   * Get all timings for this simplifier's {@link #task}.
   * @return List of timings.
   */
  public List<TaskTiming> getTimings() {
    return tracker.getTaskTimings(task);
  }
}
