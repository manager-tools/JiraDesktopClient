package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;

public class BottleneckRegressionTests extends BaseTestCase {
  private volatile int myCounter;

  protected void setUp() throws Exception {
    super.setUp();
    myCounter = 0;
  }

  public void testBottleneckMayEatRequestIfExecutionIsLong() throws InterruptedException {
    final int LONG_EXECUTION = 500;
    final int SHORT_BOTTLENECK = 200;
    final Bottleneck[] bottleneck = {null};
    bottleneck[0] = new Bottleneck(SHORT_BOTTLENECK, ThreadGate.AWT, new Runnable() {
      public void run() {
        if (myCounter == 0)
          bottleneck[0].request();
        myCounter++;
        sleep(LONG_EXECUTION);
      }
    });
    bottleneck[0].request();
    sleep((int)((LONG_EXECUTION * 2 + SHORT_BOTTLENECK) * 1.1F));
    // should be started again
    assertEquals(2, myCounter);
  }
}
