package com.almworks.timetrack.impl;

import com.almworks.util.io.persist.FormatException;
import com.almworks.util.io.persist.LeafPersistable;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A remaining time estimate, that has a timestamp (when the remaining
 * time was estimated by the user) and a value itself.
 */
public class TaskRemainingTime {
  private final long myTimestamp;
  /**
   * Seconds
   */
  private final int myEstimate;

  public static TaskRemainingTime now(int estimate) {
    return new TaskRemainingTime(System.currentTimeMillis(), estimate);
  }

  public static TaskRemainingTime old(int estimate) {
    return new TaskRemainingTime(Long.MIN_VALUE, estimate);
  }

  public TaskRemainingTime(long timestamp, int estimate) {
    myTimestamp = timestamp;
    myEstimate = estimate;
  }

  /**
   * @return seconds
   */
  public int getEstimate() {
    return myEstimate;
  }

  public long getTimestamp() {
    return myTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final TaskRemainingTime that = (TaskRemainingTime) o;
    if (myEstimate != that.myEstimate) {
      return false;
    }
    if (myTimestamp != that.myTimestamp) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (myTimestamp ^ (myTimestamp >>> 32));
    result = 31 * result + myEstimate;
    return result;
  }

  public static class Persister extends LeafPersistable<TaskRemainingTime> {
    private long myTimestamp;
    private int myEstimate;

    protected void doClear() {
      myTimestamp = myEstimate = 0;
    }

    protected TaskRemainingTime doAccess() {
      return doCopy();
    }

    protected TaskRemainingTime doCopy() {
      return new TaskRemainingTime(myTimestamp, myEstimate);
    }

    protected void doRestore(DataInput in) throws IOException, FormatException {
      myTimestamp = CompactInt.readLong(in);
      myEstimate = CompactInt.readInt(in);
    }

    protected void doSet(TaskRemainingTime value) {
      myTimestamp = value.getTimestamp();
      myEstimate = value.getEstimate();
    }

    protected void doStore(DataOutput out) throws IOException {
      CompactInt.writeLong(out, myTimestamp);
      CompactInt.writeInt(out, myEstimate);
    }
  }
}
