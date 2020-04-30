package com.almworks.util.tests;

import org.almworks.util.ExceptionUtil;

/**
 * @author dyoma
 */
public class DebugFlag {
  private boolean myValue;
  private Throwable myLastPoint = null;

  public DebugFlag(boolean initialValue) {
    myValue = initialValue;
  }

  public boolean toggleToTrue() {
    return toggle(false);
  }

  public boolean toggleToFalse() {
    return toggle(true);
  }

  private boolean toggle(boolean assertValue) {
    synchronized(this) {
      assertValue(assertValue);
      myLastPoint = new Throwable();
      myValue = !myValue;
      return true;
    }
  }

  private void assertValue(boolean assertValue) {
    assert myValue == assertValue : assertValue + "\n" + getLastPoint() + "\n";
  }

  private String getLastPoint() {
    return (myLastPoint != null ? ExceptionUtil.getStacktrace(myLastPoint) : "Never happen");
  }

  public void assertFalse() {
    synchronized(this) {
      assertValue(false);
    }
  }
}
