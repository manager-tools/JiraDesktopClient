package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.sync.ItemVersion;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;

/**
 * The {@link BigTime} implementation providing
 * total time spent on the current issue/bug
 * by all users.
 */
public class TotalTimeSpent extends TimeSpentBigTimeImpl {
  public TotalTimeSpent() {
    super(
      "Total time spent",
      Local.parse("Total time spent on this " + Terms.ref_artifact + " by all users"),
      "TotalTimeSpent");
  }

  protected Integer getStoredValue(TimeTrackingCustomizer customizer, ItemVersion item) {
    return customizer.getTimeSpent(item);
  }
}
