package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueKeyComparator;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.ArtifactBoxViewer;
import com.almworks.timetrack.gui.timesheet.GroupingFunction;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.Convertor;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * The implementation of the {@link TimeTrackingCustomizer} interface
 * using JIRA as the back-end.
 */
public class JiraTimeTrackingCustomizer implements TimeTrackingCustomizer {
  private static final String NULL_KEY = "???-???";

  private static final Comparator<ItemWrapper> ISSUE_BY_KEY_COMPARATOR =
    new Comparator<ItemWrapper>() {
      @Override
      public int compare(ItemWrapper o1, ItemWrapper o2) {
        return IssueKeyComparator.INSTANCE.compare(
          staticGetItemKey(o1), staticGetItemKey(o2));
      }
    };

  private static String staticGetItemKey(ItemWrapper a) {
    if(a == null) return NULL_KEY;
    String key = LoadedIssueUtil.getIssueKey(a);
    return key == null ? NULL_KEY : key;
  }

  @Override
  @NotNull
  public String getItemKey(@NotNull ItemVersion item) {
    return item.getNNValue(Issue.KEY, NULL_KEY);
  }

  @NotNull
  @Override
  public String getItemKey(@NotNull ItemWrapper a) {
    return MetaSchema.getNNScalarKeyValue(a, MetaSchema.KEY_KEY, String.class, NULL_KEY);
  }

  @Override
  @NotNull
  public String getItemSummary(@NotNull ItemWrapper a) {
    return Util.NN(LoadedIssueUtil.getIssueSummary(a));
  }

  @Override
  @NotNull
  public Pair<String, String> getItemKeyAndSummary(@NotNull ItemWrapper a) {
    String key = null;
    String summary = null;

    if(a != null) {
      key = LoadedIssueUtil.getIssueKey(a);
      summary = LoadedIssueUtil.getIssueSummary(a);
    }

    return Pair.create(key == null ? NULL_KEY : key, summary == null ? "" : summary);
  }

  @Override
  public Integer getRemainingTime(@NotNull ItemVersion item) {
    return item.getValue(Issue.REMAIN_ESTIMATE);
  }

  @Override
  public Integer getRemainingTime(LoadedItem item) {
    return MetaSchema.getScalarKeyValue(item, MetaSchema.KEY_REMAIN_ESTIMATE, Integer.class);
  }

  @Override
  public Integer getTimeSpent(@NotNull ItemVersion item) {
    return item.getValue(Issue.TIME_SPENT);
  }

  @Override
  public Integer getTimeSpent(LoadedItem item) {
    return MetaSchema.getScalarKeyValue(item, MetaSchema.KEY_TIME_SPENT, Integer.class);
  }

  @Override
  public Integer getTimeSpentByMe(ItemVersion item) {
    if(item == null) return null;
    ItemVersion connection = item.readValue(SyncAttributes.CONNECTION);
    if (connection == null) return null;
    long thisUser = Util.NN(connection.getValue(Connection.USER), 0l);
    if (thisUser <= 0) return null;
    int spent = 0;
    for(final ItemVersion worklog : item.readItems(item.getSlaves(Worklog.ISSUE))) {
      if ((thisUser == Util.NN(worklog.getValue(Worklog.AUTHOR), 0l)) && !worklog.getSyncState().isLocallyDeleted())
        spent += Util.NN(worklog.getValue(Worklog.TIME_SECONDS), 0);
    }
    return spent;
  }

  @Override
  @NotNull
  public List<GroupingFunction> getGroupingFunctions() {
    final List<GroupingFunction> groupings = Collections15.arrayList();
    groupings.add(TimeTrackingUtil.getConnectionGrouping());
    groupings.add(new GroupingFunction() {
      @NotNull
      @Override
      public ItemKey getGroupValue(LoadedItem item) {
        if(item == null) {
          return ItemKey.INVALID;
        }
        return Util.NN(MetaSchema.getScalarKeyValue(item, MetaSchema.KEY_PROJECT, ItemKey.class), UNKNOWN);
      }
    });
    return groupings;
  }

  @Override
  @NotNull
  public Comparator<ItemWrapper> getArtifactByKeyComparator() {
    return ISSUE_BY_KEY_COMPARATOR;
  }

  @Override
  public boolean isTimeTrackingPermissionGranted(@NotNull ItemWrapper issue) {
    return TimeUtils.canCreateWorklog(issue);
  }

  @Override
  public void publishTime(ActionContext context, Map<LoadedItem, List<TaskTiming>> timeMap,
    Map<LoadedItem, TaskRemainingTime> remMap, Map<LoadedItem, Integer> deltas, boolean upload)
    throws CantPerformException {
    final Set<LoadedItem> items = CollectionUtil.setUnion(timeMap.keySet(), remMap.keySet(), deltas.keySet());
    Engine engine = context.getSourceObject(Engine.ROLE);

    AggregatingEditCommit all = new AggregatingEditCommit();
    for(final LoadedItem a : items) {
      final List<Long> whens = Collections15.arrayList();
      final List<Integer> times = Collections15.arrayList();
      final List<String> texts = Collections15.arrayList();

      final List<TaskTiming> timings = timeMap.get(a);
      final Integer delta = deltas.get(a);

      prepareWorklogs(timings, whens, times, texts);
      assert whens.size() == times.size() && times.size() == texts.size();

      handleDelta(delta, whens, times, texts);
      assert whens.size() == times.size() && times.size() == texts.size();

      removeTooShortWorklogs(whens, times, texts);
      assert whens.size() == times.size() && times.size() == texts.size();

      final Integer newRemaining = calculateRemaining(remMap.get(a), a, timings, delta);
      EditCommit commit = UpdateIssueTimes.create(engine, a.getItem(), whens, times, texts, newRemaining, upload);
      if (commit != null) all.addProcedure(ThreadGate.LONG, commit);
    }
    context.getSourceObject(SyncManager.ROLE).unsafeCommitEdit(all);
  }

  private void prepareWorklogs(List<TaskTiming> timings, List<Long> whens, List<Integer> times, List<String> texts) {
    if(timings != null) {
      Collections.sort(timings);
      for(final TaskTiming timing : timings) {
        whens.add(timing.getStarted());
        times.add(timing.getLength());
        texts.add(timing.getComments());
      }
    }
  }

  private void handleDelta(Integer delta, List<Long> whens, List<Integer> times, List<String> texts) {
    if(delta != null && delta != 0) {
      if(whens.isEmpty()) {
        if(delta > 0) {
          whens.add(System.currentTimeMillis() - TimeTrackingUtil.millis(delta));
          times.add(delta);
          texts.add(null);
        }
      } else {
        // Proportional distribution.
        TimeTrackingUtil.distributeDelta(times, delta);
      }
    }
  }

  private void removeTooShortWorklogs(List<Long> whens, List<Integer> times, List<String> texts) {
    for(int i = times.size() - 1; i >= 0; i--) {
      if(times.get(i) < TimeTrackingUtil.MINIMAL_INTERVAL_SEC) {
        whens.remove(i);
        times.remove(i);
        texts.remove(i);
      }
    }
  }

  private Integer calculateRemaining(
    TaskRemainingTime remaining, LoadedItem a, List<TaskTiming> timings, Integer delta)
  {
    if(remaining == null && delta != null && delta != 0) {
      final Integer oldRemaining = getTimeSpent(a);
      if(oldRemaining != null) {
        remaining = TaskRemainingTime.old(oldRemaining);
      }
    }
    return TimeTrackingUtil.getRemainingTimeForTimings(timings, remaining, false);
  }

  @Override
  public JComponent createBoxViewer(Lifespan life, @NotNull ItemWrapper a) {
    if(a == null) return null;
    GuiFeaturesManager features = LoadedItemUtils.getFeatures(a);
    if (features == null) return null;
    ModelKey<String> key = MetaSchema.issueKey(features);
    ModelKey<String> summary = MetaSchema.issueSummary(features);
    if (key == null || summary == null) {
      LogHelper.error("Missing key", key, summary);
      return null;
    }
    final Convertor<String, String> identity = Convertor.identity();
    return new ArtifactBoxViewer(life, key, identity, identity, summary).getComponent();
  }
}
