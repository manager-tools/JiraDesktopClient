package com.almworks.timetrack.api;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.Role;

public interface TimeTrackerWindow {
  public static final Role<TimeTrackerWindow> ROLE = Role.role("timeTrackingWindow");

  void show();

  boolean shouldPreferTimeTrackingWindowForTray();

  String getBigTimeId();

  void setBigTimeId(String id);

  Modifiable getShowingModifiable();

  boolean isWindowShowing();
}
