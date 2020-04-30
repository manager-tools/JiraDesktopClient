package com.almworks.timetrack.impl;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.timetrack.api.UserActivityMonitor;
import com.almworks.timetrack.gui.TimeTrackerWindowComponent;
import com.almworks.timetrack.gui.TimeTrackerWindowImpl;
import com.almworks.timetrack.gui.TimeTrackingActions;
import com.almworks.timetrack.uam.UserActivityMonitorImpl;
import com.almworks.util.properties.Role;

public class TimeTrackingDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(TimeTracker.TIME_TRACKER, TimeTrackerImpl.class);
    container.registerActorClass(Role.anonymous(), TimeTrackerWindowComponent.class);
    container.registerActorClass(TimeTrackerWindow.ROLE, TimeTrackerWindowImpl.class);
    container.registerActorClass(UserActivityMonitor.ROLE, UserActivityMonitorImpl.class);
    container.registerActorClass(TimeTrackerSettings.ROLE, TimeTrackerSettingsImpl.class);
    container.registerActorClass(TimeTrackingActions.ROLE, TimeTrackingActions.class);
    container.registerActorClass(TimeItemMessage.ROLE, TimeItemMessage.class);
    container.registerActorClass(Role.anonymous(), RemovedItemsCleaner.class);
  }
}
