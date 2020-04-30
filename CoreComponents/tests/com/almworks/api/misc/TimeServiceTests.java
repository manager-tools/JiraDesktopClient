package com.almworks.api.misc;

import com.almworks.util.exec.LongEventQueue;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.threads.Computable;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TimeServiceTests extends BaseTestCase {
  private final TimeService myTimeService = new TimeService();
  private static final int PERIOD = 500;
  private int myCounter;
  private static final DateFormat MS = new SimpleDateFormat("H:m:s.S");

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myTimeService.start();
    myCounter = 0;
    LongEventQueue.installToContext();
    setWriteToStdout(true);
  }

  @Override
  protected void tearDown() throws Exception {
    myTimeService.stop();
    LongEventQueue.instance().shutdownGracefully();
    super.tearDown();
  }

  public void testBasicInvokation() throws InterruptedException {
    CountDownLatch tasks = new CountDownLatch(3);
    runTask(PERIOD, 0, tasks);
    runTask(PERIOD * 2, 1, tasks);
    runTask(PERIOD * 3, 2, tasks);
    tasks.await();
  }

  public void testExecutesLateTask() throws Exception {
    long now = System.currentTimeMillis();
    // some time has passed, so "now" is later than the one used inside the method
    Integer count = myTimeService.invokeOn(Lifespan.FOREVER, now, ThreadGate.AWT, new Computable<Integer>() {
      @Override
      public Integer compute() {
        myCounter += 1;
        return myCounter;
      }
    }).get();
    assertEquals(Integer.valueOf(1), count);
  }

  public void testCancelByLifespanEnd() throws ExecutionException, InterruptedException {
    DetachComposite life = new DetachComposite();
    Future<Integer> future = runTask(PERIOD*2, null, null, life);
    assertFalse(future.isCancelled());
    assertFalse(future.isDone());
    life.detach();
    Integer value = future.get();
    assertNull(value);
    assertTrue(future.isCancelled());
  }

  public void testCancelByFuture() throws Exception {
    Future<Integer> future = runTask(PERIOD*2, null, null, Lifespan.FOREVER);
    assertFalse(future.isCancelled());
    assertFalse(future.isDone());
    future.cancel(true);
    Integer value = future.get();
    assertNull(value);
  }

  private Future<Integer> runTask(int period, int expectedCount, CountDownLatch tasks) {
    return runTask(period, expectedCount, tasks, Lifespan.FOREVER);
  }

  private Future<Integer> runTask(final int period, final Integer expectedCount, @Nullable final CountDownLatch tasks, Lifespan life) {
    final long startTime = System.currentTimeMillis();
    final long notBefore = startTime + period;
    final Future<Integer> future = myTimeService.awtInvokeIn(life, period, new Computable<Integer>() {
      @Override
      public Integer compute() {
        long now = System.currentTimeMillis();
        assertTrue(MS.format(new Date(startTime)) + " " + period + " " + MS.format(new Date(now)), notBefore <= now);
        synchronized (TimeServiceTests.this) {
          if (expectedCount != null) {
            assertEquals(expectedCount.intValue(), myCounter);
          }
          myCounter += 1;
          return myCounter;
        }
      }
    });
    ThreadGate.LONG.execute(new Runnable() {
      @Override
      public void run() {
        try {
          Integer newCount = future.get();
          assertEquals(expectedCount != null ? expectedCount + 1 : null, newCount);
          if (tasks != null) tasks.countDown();
        } catch (Exception e) {
          fail(e.toString());
        }
      }
    });
    return future;
  }
}
