package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedBoolean;
import util.concurrent.SynchronizedLong;

public class LongEventQueueTests extends BaseTestCase {
  private final LongEventQueueEnv myQueueEnv = new LongEventQueueEnv();

  private LongEventQueueImpl myQueue;

  protected void setUp() throws Exception {
    super.setUp();
    myQueue = new LongEventQueueImpl(myQueueEnv);
  }

  protected void tearDown() throws Exception {
    myQueue.shutdownGracefully();
    myQueue = null;
    super.tearDown();
  }

  public void testStaling() throws InterruptedException {
    long TASK_DURATION = myQueueEnv.getNormalTaskDuration();
    final long SCAPEGOAT_DURATION = TASK_DURATION * 3;
    Foo[] foos = new Foo[7];
    for (int i = 0; i < foos.length; i++)
      foos[i] = new Foo();
    int scapegoat = foos.length / 2;
    foos[scapegoat] = new Foo() {
      protected void doRun() throws Exception {
        Thread.sleep(SCAPEGOAT_DURATION);
      }
    };
    for (int i = 0; i < foos.length; i++)
      myQueue.queued("" + i).execute(foos[i]);
    for (int i = 0; i < foos.length; i++) {
      long runTime = foos[i].getRunTime();
      if (i < scapegoat)
        assertTrue("(i = " + i + ") runTime = " + runTime, runTime < TASK_DURATION);
      else if (i > scapegoat)
        assertTrue("(i = " + i + ") runTime = " + runTime,
          (runTime > TASK_DURATION && runTime < SCAPEGOAT_DURATION));
      else
        assertTrue("(i = " + i + ")(scapegoat) runTime = " + runTime, runTime < SCAPEGOAT_DURATION * 3 / 2);
    }
  }

  public void testOrder() throws InterruptedException {
    long TASK_DURATION = myQueueEnv.getNormalTaskDuration();
    final long SCAPEGOAT_DURATION = TASK_DURATION * 3;
    final int CHAINS = 5;
    final int LENGTH = 5;
    Foo[][] foos = new Foo[CHAINS][];
    for (int i = 0; i < foos.length; i++) {
      foos[i] = new Foo[LENGTH];
      for (int j = 0; j < foos[i].length; j++)
        foos[i][j] = new Foo();
    }
    int scapegoatChain = 2;
    int scapegoatNumber = 2;

    foos[scapegoatChain][scapegoatNumber] = new Foo() {
      protected void doRun() throws Exception {
        Thread.sleep(SCAPEGOAT_DURATION);
      }
    };

    for (int j = 0; j < LENGTH; j++)
      for (int i = 0; i < CHAINS; i++)
        myQueue.queued("" + i).execute(true, foos[i][j]);

    for (int i = 0; i < CHAINS; i++) {
      boolean lagPassed = false;
      for (int j = 1; j < LENGTH; j++) {
        long timeDiff = foos[i][j].getRunTime() - foos[i][j - 1].getRunTime();
        assertTrue("timeDiff = " + timeDiff, timeDiff >= -10);
        if (i != scapegoatChain) {
          assertTrue("timeDiff = " + timeDiff, timeDiff < SCAPEGOAT_DURATION);
          if (timeDiff > TASK_DURATION) {
            assertFalse("(lag passed already) timeDiff = " + timeDiff, lagPassed);
            lagPassed = true;
          }
        } else {
          if (j != scapegoatNumber)
            assertTrue("timeDiff = " + timeDiff, timeDiff < TASK_DURATION);
        }
      }
    }
  }

  /**
   * See ArtifactModelRegistry listeners
   */
  public void testRunningAllSequenceInSameThread() throws InterruptedException {
    final ThreadGate gate = myQueue.queued("slowWorkers");
    final SynchronizedBoolean flag = new SynchronizedBoolean(false);
    final Synchronized<Thread> thread1 = new Synchronized<Thread>(null);
    final SynchronizedLong lockTime = new SynchronizedLong(-1);
    final SynchronizedBoolean interrupted = new SynchronizedBoolean(false);
    final Runnable unblocker = new Runnable() {
      public void run() {
        flag.set(true);
      }
    };
    Runnable blocker = new Runnable() {
      public void run() {
        gate.execute(unblocker);
        lockTime.set(System.currentTimeMillis());
        thread1.set(Thread.currentThread());
        try {
          flag.waitForValue(true);
        } catch (InterruptedException e) {
          interrupted.set(true);
        }
      }
    };
    gate.execute(blocker);
    gate.execute(unblocker);
    myQueue.queued("interrupter").execute(new Runnable() {
      public void run() {
        try {
          thread1.waitForNotNull();
          assertTrue(System.currentTimeMillis() - lockTime.get() >= myQueueEnv.getNormalTaskDuration());
        } catch (InterruptedException e) {
          fail();
        }
        thread1.get().interrupt();
      }
    });
    interrupted.waitForValue(true);
  }


  public static class Foo implements Runnable {
    private final SynchronizedBoolean myFinished = new SynchronizedBoolean(false);
    private final long myCreated = System.currentTimeMillis();
    private long myFinishedTime;

    public final void run() {
      try {
        doRun();
      } catch (Exception e) {
        // Ignore
      }
      myFinishedTime = System.currentTimeMillis();
      myFinished.set(true);
    }

    public long getRunTime() throws InterruptedException {
      myFinished.waitForValue(true);
      return myFinishedTime == 0 ? Long.MAX_VALUE : myFinishedTime - myCreated;
    }

    protected void doRun() throws Exception {
      Thread.sleep(10);
    }
  }
}
