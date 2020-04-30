package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.collections.Containers;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;

public class WorkPeriod implements Comparable<WorkPeriod> {
  public static final DataRole<WorkPeriod> ROLE =  DataRole.createRole(WorkPeriod.class);

  private final LoadedItem myItem;
  private final TaskTiming myTiming;
  private boolean myExcluded;

  public WorkPeriod(@NotNull TaskTiming timing, @NotNull LoadedItem item) {
    myTiming = timing;
    myItem = item;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    WorkPeriod that = (WorkPeriod) o;

    if (myItem != null ? !myItem.equals(that.myItem) : that.myItem != null)
      return false;
    if (myTiming != null ? !myTiming.equals(that.myTiming) : that.myTiming != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myItem != null ? myItem.hashCode() : 0;
    result = 31 * result + (myTiming != null ? myTiming.hashCode() : 0);
    return result;
  }

  public LoadedItem getArtifact() {
    return myItem;
  }

  public TaskTiming getTiming() {
    return myTiming;
  }

  public boolean isExcluded() {
    return myExcluded;
  }

  public void setExcluded(boolean excluded) {
    myExcluded = excluded;
  }

  public int compareTo(WorkPeriod o) {
    if (o == this) return 0;
    long s1 = getTiming().getStarted();
    long s2 = o.getTiming().getStarted();
    int r = Containers.compareLongs(s1, s2);
    if (r != 0) return r;
    return Containers.compareLongs(getArtifact().getItem(), o.getArtifact().getItem());
  }
}
