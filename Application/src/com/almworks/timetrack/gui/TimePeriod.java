package com.almworks.timetrack.gui;

import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Pair;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A simple structure with several utility methods to
 * simplify event time adjustments in the Time Tracker.
 */
class TimePeriod implements Comparable<TimePeriod> {
  /**
   * The enumeration of possible consequences for a
   * time period of setting a particular event time.
   */
  static enum AdjustmentStatus {
    UNTOUCHED,
    ADJUSTED,
    REMOVED
  }

  final String artifactId;
  final long started;
  final long stopped;
  final TimeTrackerTask task;
  final TaskTiming timing;
  final boolean current;

  TimePeriod(String artifactId, TimeTrackerTask task, TaskTiming timing, boolean current) {
    this.artifactId = artifactId;
    this.task = task;
    this.timing = timing;
    this.started = timing.getStarted();
    this.stopped = timing.getStopped();
    this.current = current;
  }

  public int compareTo(TimePeriod o) {
    // Sorts in reverse started time order.
    return Containers.compareLongs(o.started, started);
  }

  public String toString() {
    return artifactId + ": " + timing;
  }

  /**
   * Returns the adjustment status of this time period
   * with respect to the given event time.
   * @param oldEventTime Original event time.
   * @param newEventTime Adjusted event time.
   * @return The adjustment status.
   */
  AdjustmentStatus getAdjustmentStatus(long oldEventTime, long newEventTime) {
    if(oldEventTime >= stopped) {
      if(newEventTime >= stopped) {
        return AdjustmentStatus.UNTOUCHED;
      } else if(newEventTime > started) {
        return AdjustmentStatus.ADJUSTED;
      } else {
        return AdjustmentStatus.REMOVED;
      }
    } else if(oldEventTime <= started) {
      if(newEventTime <= started) {
        return AdjustmentStatus.UNTOUCHED;
      } else if(newEventTime < stopped) {
        return AdjustmentStatus.ADJUSTED;
      } else {
        return AdjustmentStatus.REMOVED;
      }
    }

    assert false;
    return AdjustmentStatus.UNTOUCHED;
  }

  boolean canMergeWith(TimePeriod p) {
    return task.getKey() == p.task.getKey()
          && Util.equals(timing.getComments(), p.timing.getComments());
  }

  /**
   * Splits the given list of periods into:
   * - the list of periods removed by setting the given event time (can be empty, but never null);
   * - the period adjusted by setting the given event time (there can be at most one, or null).
   * Untouched periods are skipped.
   * @param periods The list of periods to check.
   * @param oldEventTime Original event time.
   * @param newEventTime Adjusted event time.
   * @return A pair of (periods removed, period adjusted adjusted).
   */
  static Pair<List<TimePeriod>, TimePeriod> getAdjustments(
    List<TimePeriod> periods, long oldEventTime, long newEventTime)
  {
    final List<TimePeriod> removed = Collections15.arrayList();
    TimePeriod adjusted = null;

    for(final TimePeriod period : periods) {
      switch (period.getAdjustmentStatus(oldEventTime, newEventTime)) {
      case ADJUSTED:
        assert adjusted == null;
        adjusted = period;
        break;
      case REMOVED:
        removed.add(period);
        break;
      }
    }

    return Pair.create(removed, adjusted);
  }

  static Pair<TimePeriod, List<TimePeriod>> getEditedAndOthers(List<TimePeriod> periods) {
    final List<TimePeriod> others = Collections15.arrayList();
    TimePeriod edited = null;

    for(final TimePeriod p : periods) {
      if(p.current && edited == null) {
        edited = p;
      } else {
        others.add(p);
      }
    }

    return Pair.create(edited, others);
  }

  /**
   * Returns the artifact IDs of the given list of periods
   * concatenated into a string without duplicates.
   * @param periods List of periods.
   * @param noMoreThan If there's more than this number of
   * items, the string would be abbreviated. Zero means
   * no limit.
   * @return A string of all the artifact IDs.
   */
  static String getArtifactIds(List<TimePeriod> periods, int noMoreThan) {
    final Set<String> set = Collections15.linkedHashSet();
    for(final TimePeriod period : periods) {
      set.add(period.artifactId);
    }

    if(noMoreThan <= 0 || set.size() <= noMoreThan) {
      return CollectionUtil.stringJoin(set, ", ");
    } else {
      final StringBuilder sb = new StringBuilder();
      final Iterator<String> it = set.iterator();
      for(int i = 0; i < noMoreThan - 1; i++) {
        sb.append(it.next()).append(", ");
      }
      sb.append(set.size() - noMoreThan + 1).append(" more");
      return sb.toString();
    }
  }
}
