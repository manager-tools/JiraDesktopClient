package com.almworks.util.events;

public class LockOwner {
  private final String myName;
  private static final String INFIX = ":";

  public LockOwner(String constName) {
    this(constName, null);
  }

  public LockOwner(String constName, Object debugName) {
    myName = ProcessingLock.DEBUG ? debugName(constName, debugName) : String.valueOf(constName);
  }

  private String debugName(Object constName, Object debugName) {
    assert ProcessingLock.DEBUG;
    return debugName == null ? String.valueOf(constName) : constName + INFIX + debugName;
  }

  public String toString() {
    return myName;
  }
}
