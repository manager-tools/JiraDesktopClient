package com.almworks.util.events;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;

public class LifecycleAsDetachTriggerTest extends BaseTestCase {
  private Lifecycle myDetachTrigger;
  private int myDetachCounter;

  protected void setUp() throws Exception {
    myDetachTrigger = new Lifecycle(false);
  }

  protected void tearDown() throws Exception {
    myDetachTrigger = null;
  }

  public void testDetachTrigger() {
    resetCounter();
    myDetachTrigger.lifespan().add(new CounterDetach());
    assertEquals(1, myDetachCounter);
    resetCounter();

    myDetachTrigger.cycleEnd();
    assertEquals(0, myDetachCounter);

    myDetachTrigger.cycleStart();
    myDetachTrigger.lifespan().add(new CounterDetach());
    myDetachTrigger.cycleEnd();
    assertEquals(1, myDetachCounter);
  }

  private void resetCounter() {
    myDetachCounter = 0;
  }

  private final class CounterDetach extends Detach {
    protected void doDetach() {
      myDetachCounter++;
    }
  }
}
