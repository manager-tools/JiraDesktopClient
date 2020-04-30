package com.almworks.timetrack.uam;

import java.awt.*;

public class DefaultCrossPlatformLastUserActivityProvider implements LastUserActivityProvider {
  private final Point myLastMouseLocation = new Point();
  private long myLastUserActivity = 0;

  public long getLastUserActivityTime() {
    try {
      PointerInfo info = MouseInfo.getPointerInfo();
      // JCO-327: info can be null.
      if (info != null) {
        Point location = info.getLocation();
        if (myLastUserActivity == 0 || !myLastMouseLocation.equals(location)) {
          myLastUserActivity = System.currentTimeMillis();
          myLastMouseLocation.setLocation(location);
        }
      }
    } catch (HeadlessException e) {
      // ignore
    }
    return myLastUserActivity;
  }
}
