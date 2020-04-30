package com.almworks.timetrack.impl;

import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import org.almworks.util.Const;

public class TimeTrackerSettingsImpl implements TimeTrackerSettings {
  private static final boolean DEFAULT_AUTO_PAUSE_ENABLED = true;

  private final Configuration myConfig;
  private static final String ALWAYS_ON_TOP = "alwaysOnTop";
  private static final String AUTO_PAUSE_ENABLED_KEY = "autoPauseEnabled";
  private static final int DEFAULT_AUTO_PAUSE_TIME = 30;
  private static final String AUTO_PAUSE_TIME_KEY = "autoPauseTime";
  private static final boolean DEFAULT_NOTIFY_USER = true;
  private static final String NOTIFY_KEY = "notify";
  private static final int DEFAULT_FALSE_RESUME_TIMEOUT = 60;
  private static final String FALSE_RESUME_TIMEOUT_KEY = "falseResumeTimeout";
  private static final int MAX_AUTOPAUSE_MINUTES = 9999;
  private static final int MIN_AUTOPAUSE_MINUTES = 1;
  private static final int MAX_FALSE_RESUME_TIMEOUT = 9999;
  private static final int MIN_FALSE_RESUME_TIMEOUT = 1;

  public TimeTrackerSettingsImpl(Configuration config) {
    myConfig = config;
  }

  public long getAutoPauseTime() {
    boolean enabled = isAutoPauseEnabled();
    if (!enabled)
      return 0;
    int minutes = getAutoPauseTimeValue();
    return ((long) minutes) * Const.MINUTE;
  }

  public boolean isAutoPauseEnabled() {
    return myConfig.getBooleanSetting(AUTO_PAUSE_ENABLED_KEY, DEFAULT_AUTO_PAUSE_ENABLED);
  }

  public int getAutoPauseTimeValue() {
    return Math.min(MAX_AUTOPAUSE_MINUTES,
      Math.max(MIN_AUTOPAUSE_MINUTES, myConfig.getIntegerSetting(AUTO_PAUSE_TIME_KEY, DEFAULT_AUTO_PAUSE_TIME)));
  }

  public boolean isNotifyUser() {
    return myConfig.getBooleanSetting(NOTIFY_KEY, DEFAULT_NOTIFY_USER);
  }

  public long getFalseResumeTimeout() {
    int v = getFalseResumeTimeoutValue();
    return ((long) v) * Const.SECOND;
  }

  public boolean isAlwaysOnTop() {
    return myConfig.getBooleanSetting(ALWAYS_ON_TOP, !Env.isLinux());
  }

  public void setAlwaysOnTop(boolean onTop) {
    myConfig.setSetting(ALWAYS_ON_TOP, onTop);
  }

  public int getFalseResumeTimeoutValue() {
    return Math.min(MAX_FALSE_RESUME_TIMEOUT, Math.max(MIN_FALSE_RESUME_TIMEOUT,
      myConfig.getIntegerSetting(FALSE_RESUME_TIMEOUT_KEY, DEFAULT_FALSE_RESUME_TIMEOUT)));
  }

  public void setAutoPauseEnabled(boolean enabled) {
    myConfig.setSetting(AUTO_PAUSE_ENABLED_KEY, enabled);
  }

  public void setAutoPauseTimeValue(int minutes) {
    myConfig.setSetting(AUTO_PAUSE_TIME_KEY, Math.max(MIN_AUTOPAUSE_MINUTES, Math.min(MAX_AUTOPAUSE_MINUTES, minutes)));
  }

  public void setFalseResumeTimeoutValue(int seconds) {
    myConfig.setSetting(FALSE_RESUME_TIMEOUT_KEY, Math.max(MIN_FALSE_RESUME_TIMEOUT, Math.min(MAX_FALSE_RESUME_TIMEOUT, seconds)));
  }

  public void setNotifyUser(boolean value) {
    myConfig.setSetting(NOTIFY_KEY, value);
  }
}
