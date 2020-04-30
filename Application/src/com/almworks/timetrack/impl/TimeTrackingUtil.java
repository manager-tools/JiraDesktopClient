package com.almworks.timetrack.impl;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.CommonConfigurationConstants;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBEvent;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.timesheet.GroupingFunction;
import com.almworks.timetrack.gui.timesheet.WorkPeriod;
import com.almworks.util.collections.ConvertingIterator;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.FilteringIterator;
import com.almworks.util.exec.Context;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Misc Time Tracking utilities.
 */
public class TimeTrackingUtil {
  public static final long MINIMAL_INTERVAL = Const.MINUTE;
  public static final int MINIMAL_INTERVAL_SEC = seconds(MINIMAL_INTERVAL);

  /**
   * @return A {@link GroupingFunction} that groups by connection name.
   */
  public static GroupingFunction getConnectionGrouping() {
    return new GroupingFunction() {
      private final Map<Connection, ItemKey> cache = new WeakHashMap<Connection, ItemKey>();

      private ItemKey getConnectionItemKey(Connection connection) {
        ItemKey r = cache.get(connection);
        if (r != null) {
          return r;
        }

        String connectionName = null;
        if (connection != null) {
          connectionName = connection.getConfiguration().getSetting(CommonConfigurationConstants.CONNECTION_NAME, null);
        }

        if (connectionName == null) {
          r = TimeTrackingCustomizer.UNKNOWN;
        } else {
          final ItemOrder order;
          final Engine engine = Context.get(Engine.class);
          if (engine != null) {
            final ConnectionManager cm = engine.getConnectionManager();
            final List<Connection> allConnections = cm.getConnections().copyCurrent();
            order = ItemOrder.byOrder(allConnections.indexOf(connection));
          } else {
            order = ItemOrder.NO_ORDER;
          }
          r = new ItemKeyStub(connectionName, connectionName, order);
        }

        cache.put(connection, r);

        return r;
      }

      @NotNull
      public ItemKey getGroupValue(LoadedItem item) {
        if (item == null) {
          return ItemKey.INVALID;
        }
        return getConnectionItemKey(item.getConnection());
      }
    };
  }

  /**
   * @param timings List of timings.
   * @param since Timestamp.
   * @return Total elapsed time of the given timings after the timestamp.
   */
  public static int getElapsedTimeSinceForTimings(Iterable<TaskTiming> timings, long since) {
    if(timings == null) {
      return 0;
    }

    long result = 0;
    for(final TaskTiming timing : timings) {
      final long stopped = timing.getStoppedOrNow();
      if(stopped >= since) {
        result += stopped - Math.max(timing.getStarted(), since);
      }
    }

    return seconds(result);
  }

  /**
   * Returns the resulting remaining time in seconds:
   * a) get the estimate from {@code remaining};
   * b) get elapsed time for all {@code timings} after {@code remaining}'s timestamp;
   * c) calculate remaining time as MAX(a - b, 0).
   * @param timings List of timings.
   * @param remaining The remaining time estimate currently in effect.
   * @param forDisplay Whether to adjust the returned value for displaying to the user:
   * because seconds are truncated when the value is displayed, 59s are added
   * to the value when this flag is set (see comment below).
   * @return The resulting remaining time estimate, or {@code null} if {@code remaining} is {@code null}.
   */
  @Nullable
  public static Integer getRemainingTimeForTimings(
    @Nullable Iterable<TaskTiming> timings, @Nullable TaskRemainingTime remaining,
    boolean forDisplay)
  {
    if(remaining == null) {
      return null;
    }

    // We add 59 seconds here to satisfy currRemaining + currSpent = origRemaining
    // as displayed by the Time Tracker. Without it, for example:
    // - origRemaining = 2h, the user has briefly seen or remembers;
    // - currSpent = 1s, displayed as 0h;
    // - currRemaining = 1h 59m 59s displayed as 1h 59m;
    // and the user sees: 0h + 1h 59m != 2h.
    return Math.max(
      remaining.getEstimate()
        - getElapsedTimeSinceForTimings(timings, remaining.getTimestamp())
        + (forDisplay ? 59 : 0),
      0);
  }

  private static final Convertor<WorkPeriod, TaskTiming> GET_TIMING = new Convertor<WorkPeriod, TaskTiming>() {
    @Override
    public TaskTiming convert(WorkPeriod value) {
      return value.getTiming();
    }
  };

  private static final Convertor<WorkPeriod, Boolean> NOT_EXCLUDED = new Convertor<WorkPeriod, Boolean>() {
    @Override
    public Boolean convert(WorkPeriod value) {
      return !value.isExcluded();
    }
  };

  /**
   * @param periods List of periods.
   * @param since Timestamp.
   * @return Total elapsed time of all non-excluded periods in the list after the timestamp.
   */
  public static int getElapsedTimeSinceForPeriods(@Nullable Iterable<WorkPeriod> periods, long since) {
    return getElapsedTimeSinceForTimings(
      ConvertingIterator.iterable(
        FilteringIterator.iterable(
          periods,
          NOT_EXCLUDED),
        GET_TIMING),
      since);
  }

  /**
   * Returns the resulting remaining time in seconds:
   * a) get the estimate from {@code remaining};
   * b) get elapsed time for all included {@code periods} after {@code remaining}'s timestamp;
   * c) calculate remaining time as MAX(a - b, 0).
   * @param periods List of periods.
   * @param remaining The remaining time estimate currently in effect.
   * @return The resulting remaining time estimate, or {@code null} if {@code remaining} is
   * {@code null} or "don't know".
   */
  @Nullable
  public static Integer getRemainingTimeForPeriods(
    @Nullable Iterable<WorkPeriod> periods, @Nullable TaskRemainingTime remaining)
  {
    return getRemainingTimeForTimings(
      ConvertingIterator.iterable(
        FilteringIterator.iterable(
          periods,
          NOT_EXCLUDED),
        GET_TIMING),
      remaining, true);
  }

  /**
   * Convert time in milliseconds to time in seconds.
   * @param millis Duration in milliseconds.
   * @return Duration in seconds.
   */
  public static int seconds(long millis) {
    return (int)(millis / Const.SECOND);
  }

  /**
   * Convert time in seconds to time in milliseconds.
   * @param seconds Duration in seconds.
   * @return Duration in milliseconds. 
   */
  public static long millis(int seconds) {
    return seconds * Const.SECOND;
  }

  /**
   * Proportionally distribute a time delta among a list
   * of recorded durations. Modifies the {@code times} list
   * in-place.
   * @param times List of time durations.
   * @param delta The delta to distribue.
   */
  public static void distributeDelta(@NotNull List<Integer> times, int delta) {
    assert times != null && !times.isEmpty() : times;
    assert delta != 0;

    final float fdelta = delta;

    float ftotal = 0f;
    for(final int time : times) {
      ftotal += time;
    }

    int left = delta;
    for(int i = 0; i < times.size(); i++) {
      final float ftime = times.get(i);
      final int part = Math.round(fdelta * ftime / ftotal);
      times.set(i, times.get(i) + part);
      left -= part;
    }

    if(left != 0) {
      times.set(times.size() - 1, times.get(times.size() - 1) + left);
    }
  }

  public static boolean eventAffects(DBEvent event, long item) {
    return event.getAddedSorted().contains(item) || event.getChangedSorted().contains(item) || event.getRemovedSorted().contains(item);
  }
}
