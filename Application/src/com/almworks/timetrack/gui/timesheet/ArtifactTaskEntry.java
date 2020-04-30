package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Pair;
import com.almworks.util.exec.Context;

public class ArtifactTaskEntry extends TaskEntry {
  private final LoadedItem myItem;
  private final String myKey;
  private final String mySummary;

  public ArtifactTaskEntry(GroupTaskEntry parent, LoadedItem item) {
    super(parent);
    myItem = item;

    final TimeTrackingCustomizer customizer = Context.require(TimeTrackingCustomizer.ROLE);
    final Pair<String, String> kns = customizer.getItemKeyAndSummary(item);
    myKey = kns.getFirst();
    mySummary = kns.getSecond();
  }

  public String getKey() {
    return myKey;
  }

  public String getSummary() {
    return mySummary;
  }

  public LoadedItem getArtifact() {
    return myItem;
  }

  @Override
  public String toString() {
    return myKey + " " + mySummary;
  }
}

