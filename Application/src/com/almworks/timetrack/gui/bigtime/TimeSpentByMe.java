package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.sync.ItemVersion;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;

/**
 * The {@link BigTime} implementation providing
 * total time spent on the current issue/bug
 * by the current user.
 */
public class TimeSpentByMe extends TimeSpentBigTimeImpl {
  public TimeSpentByMe() {
    super(
      "Time spent by me",
      Local.parse("Total time spent on this " + Terms.ref_artifact + " by the current user"),
      "TimeSpentByMe");
  }

  protected Integer getStoredValue(TimeTrackingCustomizer customizer, ItemVersion item) {
    return customizer.getTimeSpentByMe(item);
  }
}
