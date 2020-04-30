package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.TrackerSimplifier;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base implementation for {@link TotalTimeSpent} and {@link TimeSpentByMe}
 * {@link BigTime}s.
 */
public abstract class TimeSpentBigTimeImpl extends BigTimeImpl {
  private final Lifecycle myLifecycle = new Lifecycle();
  private TimeTrackerTask myCurrTask;
  private volatile int mySpent;

  public TimeSpentBigTimeImpl(String name, String description, String id) {
    super(name, description, id);
  }

  public void getBigTimeText(Database db, TimeTracker tt, final Procedure<String> proc) {
    final TrackerSimplifier ts = new TrackerSimplifier(tt);
    if(ts.task == null) {
      proc.invoke(EMPTY_VALUE);
      return;
    }

    if(!ts.task.equals(myCurrTask)) {
      loadCommittedTime(db, ts, proc);
    } else {
      reportTotalTime(ts, proc);
    }
  }

  private void loadCommittedTime(Database db, final TrackerSimplifier ts, final Procedure<String> proc) {
    myCurrTask = ts.task;
    final TimeTrackingCustomizer customizer = Context.require(TimeTrackingCustomizer.ROLE);
    db.readForeground(new ReadTransaction<Integer>() {
      @Override
      public Integer transaction(DBReader reader) throws DBOperationCancelledException {
        ItemVersion item = SyncUtils.readTrunk(reader, ts.task.getKey());
        return getStoredValue(customizer, item);
      }
    }).finallyDo(ThreadGate.AWT, new Procedure<Integer>() {
      @Override
      public void invoke(Integer arg) {
        mySpent = Util.NN(arg, 0);
        reportTotalTime(ts, proc);
      }
    });
  }

  /**
   *
   *
   * @param customizer The {@link com.almworks.timetrack.api.TimeTrackingCustomizer} instance.
   * @param item The artifact.
   * @return The stored time spent value, to which the tracked time
   * would be added.
   */
  protected abstract Integer getStoredValue(TimeTrackingCustomizer customizer, ItemVersion item);

  private void reportTotalTime(TrackerSimplifier ts, Procedure<String> proc) {
    final List<TaskTiming> timings = ts.getTimings();

    int spent = mySpent;
    if(timings != null && !timings.isEmpty()) {
      for(final TaskTiming timing : timings) {
        spent += timing.getLength();
      }
    }

    final String text = DateUtil.getFriendlyDuration(spent, true, true);
    proc.invoke(text);
  }

  @Override
  public void attach(Database db, @Nullable final ChangeListener client) {
    myCurrTask = null;
    db.addListener(myLifecycle.lifespan(), new DBListener() {
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        if(myCurrTask != null) {
          long key = myCurrTask.getKey();
          if(TimeTrackingUtil.eventAffects(event, key)) {
            myCurrTask = null;
            if(client != null) {
              client.onChange();
            }
          }
        }
      }
    });
  }

  @Override
  public void detach() {
    myCurrTask = null;
    myLifecycle.cycle();
  }
}
