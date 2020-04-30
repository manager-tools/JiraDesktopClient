package com.almworks.util.datetime;

import java.util.Date;

public class LongDate implements Comparable<LongDate> {
  private final long myTime;

  public LongDate(long time) {
    myTime = time;
  }

  public LongDate() {
    myTime = System.currentTimeMillis();
  }

  public long toTime() {
    return myTime;
  }

  public Date toDate() {
    // Date is mutable!
    return new Date(myTime);
  }

  public int hashCode() {
    return (int)(myTime ^ (myTime >>> 32));
  }

  public boolean equals(Object obj) {
    if (obj instanceof LongDate) {
      return ((LongDate)obj).myTime == myTime;
    } else {
      return false;
    }
  }

  public String toString() {
    return Long.toString(myTime);
  }

  public int compareTo(LongDate that) {
    if (myTime > that.myTime)
      return -1;
    else if (myTime == that.myTime)
      return 0;
    else
      return 1;
  }
}
