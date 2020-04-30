package com.almworks.util.exec;

import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.SynchronizedBoolean;

public class SyncBool {
  private final SynchronizedBoolean myGo = new SynchronizedBoolean(false);
  private final SynchronizedBoolean myDone = new SynchronizedBoolean(false);
  private boolean myValue = false;


  public void waitAndSet() {
    try {
      myGo.waitForValue(true);
      myValue = true;
      myDone.set(true);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  public boolean value() {
    return myValue;
  }

  public void go() {
    try {
      myGo.set(true);
      myDone.waitForValue(true);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }
}
