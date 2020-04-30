package com.almworks.timetrack.impl;

import com.almworks.api.store.Store;
import com.almworks.api.tray.TrayIconService;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.UserActivityMonitor;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TimeTrackerImpl implements TimeTracker, Startable {
  private static final long ACTIVITY_TTL = Const.MINUTE * 2;

  private final Store myStore;
  private final TimeTrackerSettings mySettings;
  private final UserActivityMonitor myUserMonitor;
  private final TimeTrackerState myState = new TimeTrackerState();

  private final Bottleneck mySaveBottleneck = new Bottleneck(5000, ThreadGate.LONG, new Runnable() {
    public void run() {
      save();
    }
  });
  private final Timer myUserMonitorTimer = new Timer(3000, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      checkUserActivity();
    }
  });

  private final SynchronizedBoolean myLoaded = new SynchronizedBoolean(false);

  private long myLastCheckedOutagePeriodStart;

  public TimeTrackerImpl(Store store, TimeTrackerSettings settings, UserActivityMonitor userMonitor) {
    myStore = store;
    mySettings = settings;
    myUserMonitor = userMonitor;
  }

  public void start() {
    if (!myLoaded.commit(false, true))
      return;
    myState.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, mySaveBottleneck);
    Threads.assertLongOperationsAllowed();
    myState.loadFrom(myStore);
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myUserMonitorTimer.setCoalesce(true);
        myUserMonitorTimer.setRepeats(true);
        myUserMonitorTimer.start();
      }
    });
  }

  private void save() {
    if (!myLoaded.get())
      return;
    Log.debug("saving time tracking data");
    myState.saveTo(myStore);
  }

  public void stop() {
    mySaveBottleneck.abort();
    save();
  }

  public Modifiable getModifiable() {
    return myState.getModifiable();
  }

  public boolean isTracking() {
    Threads.assertAWTThread();
    return myState.isTracking();
  }

  public boolean isEmpty() {
    return myState.isEmpty();
  }

  public Map<TimeTrackerTask, List<TaskTiming>> getRecordedTimings() {
    return myState.getRecordedTimings(true);
  }

  public Map<TimeTrackerTask, List<TaskTiming>> getCurrentTimings() {
    return myState.getRecordedTimings(false);
  }

  public List<TaskTiming> getTaskTimings(TimeTrackerTask task) {
    return myState.getTaskTimings(task);
  }

  public TimeTrackerTask getLastTask() {
    return myState.getLastTask();
  }

  public void setTrackingAndCurrentTask(boolean tracking, TimeTrackerTask task) {
    myState.setTrackingAndCurrentTask(tracking, task);
  }

  public Pair<TimeTrackerTask, TaskTiming> getPrecedingTiming(long time) {
    return myState.getPrecedingTiming(time);
  }

  public boolean replaceTiming(TimeTrackerTask task, TaskTiming timing, TaskTiming newTiming) {
    return myState.replaceTiming(task, timing, newTiming);
  }

  public boolean isAutoPaused() {
    return !myState.isTracking() && myState.getCurrentTask() != null && myState.getLastAutoPauseReason() != null; 
  }

  public void removePeriod(TimeTrackerTask task, long from, long to) {
    myState.removeTimingsNoUpdate(task, from, to);
  }

  public void addTiming(TimeTrackerTask task, TaskTiming timing) {
    myState.addTiming(task, timing);
  }

  public void setTracking(boolean tracking) {
    myState.setTracking(tracking);
  }

  public TimeTrackerTask getCurrentTask() {
    return myState.getCurrentTask();
  }

  @Override
  public boolean isCurrentTaskForItem(long item) {
    final TimeTrackerTask task = getCurrentTask();
    return task != null && task.getKey() == item;
  }

  public void setRemainingTime(TimeTrackerTask task, TaskRemainingTime estimate) {
    myState.setRemainingTime(task, estimate);
  }

  public TaskRemainingTime getRemainingTime(TimeTrackerTask task) {
    return myState.getRemainingTime(task);
  }

  public Map<TimeTrackerTask, TaskRemainingTime> getRemainingTimes() {
    return myState.getRemainingTimes();
  }

  public Map<TimeTrackerTask, Integer> getSpentDeltas() {
    return myState.getSpentDeltas();
  }

  public boolean hasUnpublished(long artifactKey) {
    return myState.hasUnpublished(artifactKey);
  }

  public boolean isWindowAlwaysOnTop() {
    return mySettings.isAlwaysOnTop();
  }

  public void setSpentDelta(TimeTrackerTask task, Integer delta) {
    myState.setSpentDelta(task, delta);
  }

  private void checkUserActivity() {
    Threads.assertAWTThread();
    long now = System.currentTimeMillis();
    boolean pause = maybeSetAutoPauseOn(now);
    boolean resume = maybeSetAutoPauseOff(now);
    if (mySettings.isNotifyUser()) {
      if (resume) {
        notifyResume();
      } else if (pause) {
        notifyPause();
      }
    }
  }

  private boolean maybeSetAutoPauseOn(long now) {
    boolean tracking = myState.isTracking();
    TimeTrackerTask currentTask = myState.getCurrentTask();
    if (!tracking || currentTask == null)
      return false;
    long autoPauseTime = mySettings.getAutoPauseTime();
    if (autoPauseTime <= 0)
      return false;
    TaskTiming timing = myState.getLastTiming(currentTask);
    if (timing == null || timing.getStopped() > 0)
      return false;

    long falseResumeTimeout = mySettings.getFalseResumeTimeout();

    UserActivityMonitor.OutagePeriod period = myUserMonitor.getLastOutagePeriod();
    if (period != null && period.getStarted() > myLastCheckedOutagePeriodStart &&
      period.getEnded() - period.getStarted() > autoPauseTime)
    {
      myLastCheckedOutagePeriodStart = period.getStarted();
      long timePassed = now - period.getEnded();
      if (timePassed > 0 && timePassed <= ACTIVITY_TTL) {
        if (timing.getStarted() <= period.getStarted()) {
          AutoPauseReason reason =
            period.isAppQuit() ? AutoPauseReason.APPLICATION_INACTIVE : AutoPauseReason.COMPUTER_INACTIVE;
          Log.debug("TTI: auto-pausing due to " + reason + " at " + new Date(period.getStarted()) + " (period end: " + period.getEnded() + ", now: " + now + ")");
          myState.setAutoPaused(period.getStarted(), reason, falseResumeTimeout);
          return true;
        }
      }
    }

    long userActivity = myUserMonitor.getLastUserActivityTime();
    if (userActivity > 0 && (now - userActivity) > autoPauseTime) {
      if (timing.getStarted() <= userActivity) {
        Log.debug("TTI: auto-pausing due to " + AutoPauseReason.USER_INACTIVE + " at " + new Date(userActivity));
        myState.setAutoPaused(userActivity, AutoPauseReason.USER_INACTIVE, falseResumeTimeout);
        return true;
      }
    }
    return false;
  }

  private boolean maybeSetAutoPauseOff(long now) {
    boolean tracking = myState.isTracking();
    if (tracking)
      return false;
    TimeTrackerTask currentTask = myState.getCurrentTask();
    if (currentTask == null)
      return false;
    AutoPauseReason autoPauseReason = myState.getLastAutoPauseReason();
    if (autoPauseReason == null)
      return false;
    TaskTiming timing = myState.getLastTiming(currentTask);
    if (timing == null || timing.getStopped() <= 0)
      return false;
    long activityTime = myUserMonitor.getLastUserActivityTime();
    if (activityTime > now || (now - activityTime) > ACTIVITY_TTL) {
      // sanity checks - probably remove?
      return false;
    }
    if (activityTime > timing.getStopped() && Math.abs(activityTime - myState.getLastAutoPauseTime()) > 100) {
      Log.debug("TTI: auto-unpausing at " + new Date(activityTime));
      myState.resumeFromAutoPause(activityTime);
      return true;
    }
    return false;
  }

  private void notifyPause() {
    TrayIconService trayIcon = Context.get(TrayIconService.class);
    if (trayIcon == null)
      return;
    if (myState.isTracking())
      return;
    AutoPauseReason reason = myState.getLastAutoPauseReason();
    if (reason == null)
      return;
    TimeTrackerTask task = myState.getCurrentTask();
    if (task == null)
      return;
    TaskTiming timing = myState.getLastTiming(task);
    if (timing == null)
      return;
    long stopped = timing.getStopped();
    if (stopped <= 0)
      return;

    String caption = "Time Tracking Paused";
    String time = toTimeDate(stopped);
    String message = Local.parse(Terms.ref_Deskzilla) + " has auto-paused time tracking\nat " + time + " due to " +
      reason.getReason() + ".";

    trayIcon.displayMessage(caption, message);
  }

  private static String toTimeDate(long stopped) {
    Date d = new Date(stopped);
    String time = DateUtil.LOCAL_TIME.format(d);
    if (!DateUtil.isSameDay(stopped, System.currentTimeMillis())) {
      time += " on " + DateUtil.LOCAL_DATE.format(d);
    }
    return time;
  }

  private void notifyResume() {
    TrayIconService trayIcon = Context.get(TrayIconService.class);
    if (trayIcon == null)
      return;
    if (!myState.isTracking())
      return;
    AutoPauseReason reason = myState.getLastAutoPauseReason();
    if (reason == null)
      return;
    TimeTrackerTask task = myState.getCurrentTask();
    if (task == null)
      return;
    List<TaskTiming> timings = myState.getTaskTimings(task);
    if (timings == null || timings.isEmpty())
      return;
    long pauseTime = myState.getLastAutoPauseTime();
    long from = 0;
    long to = 0;
    if (timings.size() >= 2) {
      int tsz = timings.size();
      TaskTiming a = timings.get(tsz - 2);
      TaskTiming b = timings.get(tsz - 1);
      // not tracking?
      if (b.getStopped() > 0)
        return;
      if (Math.abs(a.getStopped() - pauseTime) < TimeTrackingUtil.MINIMAL_INTERVAL) {
        from = a.getStopped();
        to = b.getStarted();
      }
    }

    String caption = "Time Tracking Resumed";
    String message = Local.parse(Terms.ref_Deskzilla) + " has recorded a pause in time tracking\n";
    if (from > 0 && to > 0) {
      message += "from " + toTimeDate(from) + " to " + toTimeDate(to) + " ";
    }
    message += "due to " + reason.getReason() + ".";

    trayIcon.displayMessage(caption, message);
  }

  @Override
  public void clearStateForItem(long item) {
    myState.clearStateForItem(item);
  }
}
