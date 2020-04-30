package com.almworks.items.api;

import org.almworks.util.Util;

public final class DBPriority implements Comparable<DBPriority> {
  public static final DBPriority BACKGROUND = new DBPriority(false, 0);
  public static final DBPriority FOREGROUND = new DBPriority(true, 0);
  
  private final boolean myForeground;
  private final int myRelativePriority;

  public DBPriority(boolean foreground, int relativePriority) {
    myForeground = foreground;
    myRelativePriority = relativePriority;
  }

  public boolean isForeground() {
    return myForeground;
  }

  public int getRelativePriority() {
    return myRelativePriority;
  }

  public int toBackgroundPriority() {
    return myRelativePriority + (myForeground ? 5 : 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    DBPriority that = (DBPriority) o;

    if (myForeground != that.myForeground)
      return false;
    if (myRelativePriority != that.myRelativePriority)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (myForeground ? 1 : 0);
    result = 31 * result + myRelativePriority;
    return result;
  }

  @Override
  public int compareTo(DBPriority o) {
    if (o == null) return -1;
    return Util.compareInts(o.toBackgroundPriority(), toBackgroundPriority());
  }

  @Override
  public String toString() {
    String r = myForeground ? "F" : "B";
    if (myRelativePriority != 0) {
      r += "[" + myRelativePriority + "]";
    }
    return r;
  }
}
