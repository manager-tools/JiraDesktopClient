package com.almworks.util.collections;

import junit.framework.Assert;

/**
 * @author dyoma
 */
public class ChangeCounter implements ChangeListener {
  private int myCounter = 0;

  public void onChange() {
    myCounter++;
  }

  public int getCount() {
    return myCounter;
  }

  public void reset() {
    myCounter = 0;
  }

  public void assertIncremented() {
    Assert.assertTrue(myCounter > 0);
    reset();
  }

  public void assertNotCalled() {
    Assert.assertEquals(0, myCounter);
  }
}
