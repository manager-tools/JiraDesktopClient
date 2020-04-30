package com.almworks.timetrack.gui;

import com.almworks.api.gui.MainMenu;
import com.almworks.api.tray.TrayIconService;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionRegistry;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

import java.awt.*;

public class TimeTrackerWindowComponent implements Startable {
  private final ActionRegistry myActionRegistry;
  private final TimeTrackerSettings mySettings;
  private final TimeTracker myTimeTracker;
  private final TrayIconService myTrayIconService;

  public TimeTrackerWindowComponent(ActionRegistry actionRegistry, TimeTrackerSettings settings, TimeTracker timeTracker, TrayIconService trayIconService) {
    myActionRegistry = actionRegistry;
    mySettings = settings;
    myTimeTracker = timeTracker;
    myTrayIconService = trayIconService;
  }

  public void start() {
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING, new OpenTimeTrackerWindowAction());
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_START, TimeTrackingControlAction.START);
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_PAUSE, TimeTrackingControlAction.PAUSE);
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_STOP, TimeTrackingControlAction.STOP);
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_OPTIONS, new TimeTrackingOptionsAction(mySettings));

    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myTimeTracker.getModifiable().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
          public void onChange() {
            updateTrayFromTracker();
          }
        });
        updateTrayFromTracker();
      }
    });
  }

  private void updateTrayFromTracker() {
    boolean tracking = myTimeTracker.isTracking();
    boolean hasTask = myTimeTracker.getCurrentTask() != null;
    Image image = !hasTask ? null : (tracking ? Icons.APPLICATION_LOGO_ICON_SMALL_STARTED.getImage() :
      Icons.APPLICATION_LOGO_ICON_SMALL_PAUSED.getImage());
    myTrayIconService.setTrayImage(image);
  }

  public void stop() {

  }
}
