package com.almworks.timetrack.api;

import com.almworks.util.properties.Role;

public interface TimeTrackerSettings {
  Role<TimeTrackerSettings> ROLE = Role.role("timeTrackingSettings");

  boolean isNotifyUser();

  int getAutoPauseTimeValue();

  int getFalseResumeTimeoutValue();

  boolean isAutoPauseEnabled();

  void setAutoPauseEnabled(boolean enabled);

  void setAutoPauseTimeValue(int minutes);

  void setFalseResumeTimeoutValue(int seconds);

  void setNotifyUser(boolean value);

  /**
   * @return number of milliseconds of inactivity that should lead to autopause, or 0 if turned off
   */
  long getAutoPauseTime();

  long getFalseResumeTimeout();

  boolean isAlwaysOnTop();

  void setAlwaysOnTop(boolean onTop);
}
