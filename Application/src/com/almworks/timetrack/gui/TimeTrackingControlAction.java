package com.almworks.timetrack.gui;

import com.almworks.timetrack.api.TimeTracker;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

public class TimeTrackingControlAction extends SimpleAction {
  public static final AnAction START =
    new TimeTrackingControlAction("Start Tracking", Icons.ACTION_TIME_START_LARGE, "control 8", "Start tracking time spent on the current task");
  public static final AnAction PAUSE =
    new TimeTrackingControlAction("Pause Tracking", Icons.ACTION_TIME_PAUSE_LARGE, "control 9", "Pause tracking time spent on the current task");
  public static final AnAction STOP =
    new TimeTrackingControlAction("Stop Tracking", Icons.ACTION_TIME_STOP_LARGE, "control 0", "Stop tracking time");

  private TimeTrackingControlAction(String name, Icon icon, String shortcut, String desc) {
    super(name, icon);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(shortcut));
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, desc);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    context.updateOnChange(tt.getModifiable());

    boolean hasWork = tt.getCurrentTask() != null;
    boolean hasLastWork = tt.getLastTask() != null;
    boolean tracking = tt.isTracking();

    // antipolimorphism!
    if (this == STOP) {
      context.setEnabled(hasWork);
    } else if (this == START) {
      context.setEnabled(hasLastWork || hasWork);
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, hasWork);
    } else if (this == PAUSE) {
      context.setEnabled(hasWork);
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, hasWork && !tracking);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    if (this == STOP) {
      tt.setTrackingAndCurrentTask(false, null);
    } else if (this == PAUSE) {
      if (tt.isTracking()) {
        tt.setTracking(false);
      } else if (tt.getCurrentTask() != null) {
        tt.setTracking(true);
      }
    } else if (this == START) {
      tt.setTrackingAndCurrentTask(true, tt.getLastTask());
    }
  }
}
