package com.almworks.timetrack.gui;

import com.almworks.api.gui.MainMenu;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.picocontainer.Startable;

/**
 * Actions for the Time Tracking facility.
 */
public class TimeTrackingActions implements Startable {
  public static final Role<TimeTrackingActions> ROLE = Role.role(TimeTrackingActions.class);

  public static final AnAction START_WORK = new StartWorkAction();
  public static final AnAction STOP_WORK = new StopWorkAction();
  public static final AnAction PUBLISH_TIME = new PublishTimeAction();

  private final ActionRegistry myActionRegistry;

  public TimeTrackingActions(ActionRegistry actionRegistry) {
    myActionRegistry = actionRegistry;
  }

  public void start() {
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_START_WORK_ON_ISSUE, START_WORK);
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_STOP_WORK_ON_ISSUE, STOP_WORK);
    myActionRegistry.registerAction(MainMenu.Tools.TIME_TRACKING_PUBLISH, PUBLISH_TIME);
    myActionRegistry.registerAction(MainMenu.X_PUBLISH_TIME, PUBLISH_TIME);
  }

  public void stop() {
  }
}
