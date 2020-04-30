package com.almworks.timetrack.impl;

import com.almworks.util.collections.Containers;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.io.persist.FormatException;
import com.almworks.util.io.persist.LeafPersistable;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

public final class TaskTiming implements Comparable<TaskTiming> {
  private final long myStarted;

  /**
   * Time tracking stopped, if <=0 -- tracking is still going on
   */
  private final long myStopped;

  private final String myComments;

  private final boolean myCurrent;

  public TaskTiming(long started, long stopped, String comments, boolean current) {
    assert stopped <= 0 || started <= stopped : started + " " + stopped;
    myStarted = started;
    myStopped = stopped;
    myComments = comments;
    myCurrent = current;
  }

  public TaskTiming(long started, long stopped, String comments) {
    this(started, stopped, comments, false);
  }

  @Override
  public String toString() {
    return "[" + DateUtil.LOCAL_DATE_TIME.format(new Date(myStarted)) + " to " + (myStopped <= 0 ? "now" : DateUtil.LOCAL_DATE_TIME.format(new Date(myStopped))) + (myComments == null ? "" : " : " + myComments) + "]";
  }

  public long getStarted() {
    return myStarted;
  }

  public long getStopped() {
    return myStopped;
  }

  public long getStoppedOrNow() {
    return myStopped <= 0 ? System.currentTimeMillis() : myStopped;
  }

  public int getLength() {
    return TimeTrackingUtil.seconds(getStoppedOrNow() - myStarted);
  }

  public String getComments() {
    return myComments;
  }

  public boolean isCurrent() {
    return myStopped <= 0 || myCurrent;
  }

  public int compareTo(TaskTiming o) {
    if (this == o)
      return 0;
    return Containers.compareLongs(myStarted, o.myStarted);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TaskTiming timing = (TaskTiming) o;

    if (myStarted != timing.myStarted)
      return false;
    if (myStopped != timing.myStopped)
      return false;
    if (myComments != null ? !myComments.equals(timing.myComments) : timing.myComments != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (myStarted ^ (myStarted >>> 32));
    result = 31 * result + (int) (myStopped ^ (myStopped >>> 32));
    result = 31 * result + (myComments != null ? myComments.hashCode() : 0);
    return result;
  }

  public static class Persister extends LeafPersistable<TaskTiming> {
    private static final int MAX_COMMENTS_LENGTH = 16000;
    private long myStarted;
    private long myStopped;
    private String myComments;

    protected void doClear() {
      myStarted = myStopped = 0;
      myComments = null;
    }

    protected TaskTiming doAccess() {
      return doCopy();
    }

    protected TaskTiming doCopy() {
      return new TaskTiming(myStarted, myStopped, myComments);
    }

    protected void doRestore(DataInput in) throws IOException, FormatException {
      myStarted = CompactInt.readLong(in);
      myStopped = CompactInt.readLong(in);
      myComments = CompactChar.readString(in, MAX_COMMENTS_LENGTH);
    }

    protected void doSet(TaskTiming value) {
      myStarted = value.getStarted();
      myStopped = value.getStopped();
      myComments = value.getComments();
      if (myComments != null && myComments.length() > MAX_COMMENTS_LENGTH)
        myComments = myComments.substring(0, MAX_COMMENTS_LENGTH);
    }

    protected void doStore(DataOutput out) throws IOException {
      CompactInt.writeLong(out, myStarted);
      CompactInt.writeLong(out, myStopped);
      CompactChar.writeString(out, myComments);
    }
  }
}

