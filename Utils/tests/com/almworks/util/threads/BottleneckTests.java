package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;

public class BottleneckTests extends BaseTestCase {
  private volatile int myCounter;
  private Bottleneck myBottleneck;
  private static final int PERIOD = 500;
  private long myInitTime;

  protected void tearDown() throws Exception {
    myBottleneck = null;
    super.tearDown();
  }

  protected void setUp() throws Exception {
    super.setUp();
    myCounter = 0;
    myInitTime = System.currentTimeMillis();
    myBottleneck = new Bottleneck(PERIOD, ThreadGate.STRAIGHT, new Runnable() {
      public void run() {
        myCounter++;
        System.out.println(System.currentTimeMillis() - myInitTime);
        new Throwable().printStackTrace();
      }
    });
  }

  public void testSetNextTimeKeepsBottleneckFromRunning() throws InterruptedException {
    Thread updater = new Thread() {
      public void run() {
        try {
          while (!Thread.interrupted()) {
            myBottleneck.requestDelayed();
            Thread.sleep(PERIOD / 2);
          }
        } catch (InterruptedException e) {
          // finish
        }
      }
    };
    updater.start();
    try {
      Thread.sleep(PERIOD * 7 / 2);
      assertEquals(0, myCounter);
      updater.interrupt();
      Thread.sleep(PERIOD * 2);
      assertEquals(1, myCounter);
    } finally {
      updater.interrupt();
      updater.join();
    }
  }

  public void testBasicBottlenecking() throws InterruptedException {
    int COUNT = 20;
    int SLEEP = PERIOD / 4;
    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
      myBottleneck.request();
      myBottleneck.request();
      Thread.sleep(SLEEP);
    }
    int counter = myCounter;
    long time = System.currentTimeMillis() - start;
    assertTrue("" + counter, counter <= time / PERIOD + 1);
  }

  public void testRequestingWhileRunningMakesItRunOnceAgain() {
    final boolean[] running = {false};
    final int DURATION = 300;
    myBottleneck = new Bottleneck(PERIOD, ThreadGate.AWT, new Runnable() {
      public void run() {
        running[0] = true;
        myCounter++;
        sleep(DURATION);
        running[0] = false;
      }
    });
    myBottleneck.request();
    sleep(DURATION / 3);
    assertTrue(running[0]);
    myBottleneck.request();
    sleep(DURATION);
    assertFalse(running[0]);
    sleep(PERIOD + DURATION);
    assertFalse(running[0]);
    assertEquals(2, myCounter);
  }
}
