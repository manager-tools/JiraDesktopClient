package com.almworks.util.debug;

public class DebugCounter extends RuntimeDebug {
  protected volatile long myCount;
  protected final String myName;

  public DebugCounter(String name) {
    myName = name;
  }

  public DebugCounter increment() {
    myCount++;
    return this;
  }

  public DebugCounter add(long value) {
    myCount += value;
    return this;
  }

  public DebugCounter dump(final long period) {
    return (DebugCounter) super.dump(period);
  }

  protected boolean hasStats() {
    return myCount > 0;
  }

  public void clear() {
    myCount = 0;
  }

  protected void output() {
    output(myName + "=" + myCount + "; ");
  }

  public String toString() {
    return myName;
  }
}
