package com.almworks.util.collections;

import org.almworks.util.RuntimeInterruptedException;

public class TestChangeListener implements ChangeListener {
  private boolean mySignalled;

  public void onChange() {
    synchronized(this) {
      mySignalled = true;
      notifyAll();
    }
  }

  public boolean waitChange(long timeout) {
    try {
      synchronized(this) {
        if (!mySignalled)
          wait(timeout);
        boolean signalled = mySignalled;
        mySignalled = false;
        return signalled;
      }
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }
}
