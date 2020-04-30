package com.almworks.timetrack.api;

import com.almworks.api.application.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.timetrack.gui.timesheet.GroupingFunction;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Pair;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This interface represents the layer of abstraction that
 * shields the Time Tracking facility from tracker-specific
 * details.
 */
public interface TimeTrackingCustomizer {
  Role<TimeTrackingCustomizer> ROLE = Role.role("TimeTrackingCustomizer", TimeTrackingCustomizer.class);
  
  ItemKey UNKNOWN = new ItemKeyStub("unknown", "Unknown", ItemOrder.NO_ORDER);

  /**
   * Returns a String representation of the artifact key
   * given an {@link ItemWrapper}.
   *
   * @param a The artifact wrapper.
   * @return The atifact key as a string.
   */
  @NotNull String getItemKey(@NotNull ItemVersion a);

  @NotNull String getItemKey(@NotNull ItemWrapper a);

  /**
   * Returns the artifact summary as a {@link String}
   * given an {@link ItemWrapper}.
   * @param a The artifact wrapper.
   * @return The artifact summary as a {@code String}.
   */
  @NotNull String getItemSummary(@NotNull ItemWrapper a);

  /**
   * Returns a {@link Pair} containing both key and summary
   * given an {@link ItemWrapper}. Introduced for
   * performance reasons.
   * @param a The artifact wrapper.
   * @return The pair containing the artifact key and summary.
   */
  @NotNull Pair<String, String> getItemKeyAndSummary(@NotNull ItemWrapper a);

  /**
   * Returns the remaining time estimate in seconds
   * for the given {@link ItemWrapper}.
   *
   * @param item@return The remainig time estimate or {@code null} if it is
   * not set or unavailable.
   */
  @Nullable Integer getRemainingTime(@NotNull ItemVersion item);

  @Nullable
  Integer getRemainingTime(LoadedItem item);

  /**
   * Returns the time spent in seconds for the given
   * {@link ItemWrapper}.
   *
   * @param a The artifact wrapper.
   * @return The time spent or {@code null} if it is
   * not set or unavailable.
   */
  @Nullable Integer getTimeSpent(@NotNull ItemVersion a);

  @Nullable
  Integer getTimeSpent(LoadedItem item);

  /**
   * Returns the time spent by the current user on the
   * given artifact (in seconds).
   *
   * @param a The artifact wrapper.
   * @return The time spent by the current user or
   * {@code null} if it is not set or unavailable.
   */
  @Nullable Integer getTimeSpentByMe(ItemVersion a);

  /**
   * Returns a list of {@link GroupingFunction}s to be used
   * in the timesheet.
   * @return The list of grouping functions.
   */
  @NotNull List<GroupingFunction> getGroupingFunctions();

  /**
   * Returns a {@link Comparator} instance that compares
   * {@link ItemWrapper}s by their artifact keys.
   * @return The comparator.
   */
  @NotNull Comparator<ItemWrapper> getArtifactByKeyComparator();

  /**
   * Determines whether the user has the permission to
   * track and log time for the given artifact.
   * @param a The artifact wrapper.
   * @return {@code true} if the user has the needed permission;
   * {@code false} otherwise.
   */
  boolean isTimeTrackingPermissionGranted(@NotNull ItemWrapper a);

  /**
   * Create the component to show in the Time Tracker as the current task.<br>
   * @param life the life of viewer. This life ends when the viewer won't be used anymore
   * @param a The artifact wrapper.
   * @return The component wrapper.
   */
  @Nullable
  JComponent createBoxViewer(Lifespan life, @NotNull ItemWrapper a);

  /**
   * @param timeMap Timings map.
   * @param remMap Remaining times map.
   * @param deltas Time spent deltas map.
   * @param upload Upload immediately or just save draft.
   */
  void publishTime(ActionContext context,
    Map<LoadedItem, List<TaskTiming>> timeMap, Map<LoadedItem, TaskRemainingTime> remMap,
    Map<LoadedItem, Integer> deltas, boolean upload) throws CantPerformException;
}
