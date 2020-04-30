package com.almworks.timetrack.gui;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.List;
import java.util.*;

class ChangeLastEventTimeAction extends SimpleAction {
  private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);
  private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);

  ChangeLastEventTimeAction() {
    super("Work stopped");
    watchRole(TimeTracker.TIME_TRACKER);
    TimeTrackerForm.TRACKING_TICKS.install(this);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Adjust time and duration");
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final TrackerSimplifier ts = new TrackerSimplifier(context);
    context.updateOnChange(ts.tracker.getModifiable());

    if(ts.task == null) {
      context.setEnabled(false);
      return;
    }

    final StringBuilder name = new StringBuilder();
    name.append("<html><center>Work ");

    final TaskTiming timing = ts.getLastTiming();
    final long time;
    if(timing != null) {
      time = ts.tracking ? timing.getStarted() : timing.getStopped();
      assert time != 0;
    } else {
      time = 0;
    }

    if (ts.currTask == null) {
      name.append("stopped");
    } else if (ts.tracking) {
      boolean resumed = false;
      if (time != 0) {
        final Pair<TimeTrackerTask, TaskTiming> pair = ts.tracker.getPrecedingTiming(time);
        if (pair != null && Util.equals(pair.getFirst(), ts.currTask)) {
          resumed = true;
        }
      }
      name.append(resumed ? "resumed" : "started");
    } else {
      name.append(ts.tracker.isAutoPaused() ? "auto-paused" : "paused");
    }

    if (time != 0) {
      name.append(" at ");
      final Date d = new Date(time);
      name.append(TIME_FORMAT.format(d));

      final TimeZone tz = TimeZone.getDefault();
      final long nowDay = DateUtil.toDayStart(System.currentTimeMillis(), tz);
      final long thenDay = DateUtil.toDayStart(time, tz);

      if (nowDay != thenDay) {
        if (nowDay - thenDay == Const.DAY) {
          name.append(" yesterday");
        } else {
          name.append(' ').append(DATE_FORMAT.format(d));
        }
      }
    }

    name.append("<br>Time spent: ");
    if (timing != null) {
      name.append(DateUtil.getFriendlyDuration(timing.getLength(), false));
      TimeTrackerUIConsts.setAffordanceIcon(context.getComponent());
    } else {
      name.append("none");
      TimeTrackerUIConsts.setNullIcon(context.getComponent());
    }

    context.setEnabled(time != 0);
    context.putPresentationProperty(PresentationKey.NAME, name.toString());
  }

  @Override
  protected void doPerform(final ActionContext context) throws CantPerformException {
    final TrackerSimplifier ts = new TrackerSimplifier(context);
    final TimeTrackerTask currTask = ts.task;
    if(currTask == null) {
      return;
    }

    final TimeTrackingCustomizer ttc = Context.require(TimeTrackingCustomizer.ROLE);
    final Map<TimeTrackerTask, List<TaskTiming>> records = ts.tracker.getCurrentTimings();

    context.getSourceObject(Database.ROLE).readForeground(new ReadTransaction<List<TimePeriod>>() {
      @Override
      public List<TimePeriod> transaction(DBReader reader) throws DBOperationCancelledException {
        return loadPeriods(records, ttc, currTask, reader);
      }
    }).finallyDo(ThreadGate.AWT, new Procedure<List<TimePeriod>>() {
      @Override
      public void invoke(List<TimePeriod> periods) {
        showForm(context.getComponent(), periods, !ts.tracking);
      }
    });
  }

  private List<TimePeriod> loadPeriods(Map<TimeTrackerTask, List<TaskTiming>> records, TimeTrackingCustomizer ttc,
    TimeTrackerTask currTask, DBReader reader)
  {
    final List<TimePeriod> periods = Collections15.arrayList();
    for(final Map.Entry<TimeTrackerTask, List<TaskTiming>> e : records.entrySet()) {
      final TimeTrackerTask task = e.getKey();
      ItemVersion item = SyncUtils.readTrunk(reader, task.getKey());
      final String id = ttc.getItemKey(item);
      final List<TaskTiming> timings = e.getValue();
      for(final TaskTiming timing : timings) {
        periods.add(new TimePeriod(id, task, timing, currTask.equals(task)));
      }
    }
    Collections.sort(periods);
    return periods;
  }

  private void showForm(Component parentComponent, final List<TimePeriod> periods, final boolean end)
  {
    final Pair<TimePeriod, List<TimePeriod>> pair = TimePeriod.getEditedAndOthers(periods);
    final TimePeriod period = pair.getFirst();
    final List<TimePeriod> others = pair.getSecond();
    if(period == null) {
      return; // nothing to edit
    }

    final ChangeEventTimeForm form = new ChangeEventTimeForm(end, period, others);
    final JDialog dialog = BaseAdjustmentForm.setupFormDialog(form, parentComponent);
    if(dialog != null) {
      form.attach(new Procedure<Long>() {
        public void invoke(Long arg) {
          apply(dialog, arg, period, others, end);
        }
      });
      dialog.pack();
      dialog.show();
      dialog.requestFocus();
    }
  }

  private void apply(JDialog dialog, Long newTime, TimePeriod period, List<TimePeriod> others, boolean end) {
    dialog.dispose();
    
    if (newTime == null) {
      return;
    }

    final TimeTracker tt = Context.get(TimeTracker.class);
    if (tt == null) {
      return;
    }

    assert period != null;

    final long originalTime;
    final boolean canAdjustOthers;

    if(end) {
      originalTime = period.stopped;
      canAdjustOthers = newTime > originalTime;
    } else {
      originalTime = period.started;
      canAdjustOthers = newTime < originalTime;
    }

    if (canAdjustOthers) {
      final Pair<List<TimePeriod>, TimePeriod> listPair =
        TimePeriod.getAdjustments(others, originalTime, newTime);

      // Removing previous periods as necessary.
      for (final TimePeriod p : listPair.getFirst()) {
        tt.removePeriod(p.task, p.started, p.stopped);
      }

      // Adjusting the most recent remaining period if necessary.
      final TimePeriod adjusted = listPair.getSecond();

      if (adjusted != null) {
        if(adjusted.canMergeWith(period)) {
          // Merge two periods.
          tt.removePeriod(adjusted.task, adjusted.started, adjusted.stopped);
          adjustPeriod(tt, end, period, end ? adjusted.stopped : adjusted.started);
        } else {
          // Adjust two periods, keeping them separate.
          adjustPeriod(tt, !end, adjusted, newTime);
          adjustPeriod(tt, end, period, newTime);
        }
      } else {
        // Adjusting the current period.
        adjustPeriod(tt, end, period, newTime);
      }
    } else {
      switch (period.getAdjustmentStatus(originalTime, newTime)) {
      case REMOVED:
        tt.removePeriod(period.task, period.started, period.stopped);
        break;

      default:
        adjustPeriod(tt, end, period, newTime);
        break;
      }
    }
  }

  private void adjustPeriod(TimeTracker tt, boolean end, TimePeriod period, Long newTime) {
    final long startTime;
    final long endTime;
    if(end) {
      startTime = period.started;
      endTime = newTime;
    } else {
      startTime = newTime;
      endTime = period.stopped;
    }

    final TaskTiming t = new TaskTiming(startTime, endTime, period.timing.getComments());
    if (!t.equals(period.timing)) {
      tt.replaceTiming(period.task, period.timing, t);
    }
  }
}
