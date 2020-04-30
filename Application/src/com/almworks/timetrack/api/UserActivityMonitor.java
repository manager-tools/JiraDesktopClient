package com.almworks.timetrack.api;

import com.almworks.util.properties.Role;

public interface UserActivityMonitor {
  Role<UserActivityMonitor> ROLE = Role.role("userActivityMonitor");

  OutagePeriod getLastOutagePeriod();

  long getLastUserActivityTime();

  class OutagePeriod {
    private final long myStarted;
    private final long myEnded;
    private final boolean myAppQuit;

    public OutagePeriod(long started, long ended, boolean appQuit) {
      myStarted = started;
      myEnded = ended;
      myAppQuit = appQuit;
    }

    public long getStarted() {
      return myStarted;
    }

    public long getEnded() {
      return myEnded;
    }

    public boolean isAppQuit() {
      return myAppQuit;
    }
  }
}
