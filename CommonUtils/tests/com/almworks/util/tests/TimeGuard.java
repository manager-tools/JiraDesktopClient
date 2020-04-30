package com.almworks.util.tests;

import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ImmediateThreadGate;
import junit.framework.Assert;
import org.almworks.util.ExceptionUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class TimeGuard<T> {
  private static AtomicBoolean ourIgnoreTimeout = new AtomicBoolean(false);
  private static final int TIME_OUT = 300;
  private final Procedure<TimeGuard<T>> myCheck;
  private boolean myHasResult = false;
  private T myResult = null;
  private static final int WAIT_PERIOD = 50;
  private static final int MIN_RETRIES = 10;

  public TimeGuard(Procedure<TimeGuard<T>> check) {
    myCheck = check;
  }

  public static <T> T waitFor(Procedure<TimeGuard<T>> check) throws InterruptedException {
    return new TimeGuard<T>(check).waitAndGet();
  }

  /**
   * Blocks current thread until the result is set.<br>
   * Blocks gate thread for each execution of <tt>check</tt>.
   */
  public static <T> T waitFor(final ImmediateThreadGate gate, final Procedure<TimeGuard<T>> check) throws InterruptedException {
    final Throwable[] exception = new Throwable[1];
    T result = waitFor(new Procedure<TimeGuard<T>>() {
      @Override
      public void invoke(final TimeGuard<T> guard) {
        gate.execute(new Runnable() {
          @Override
          public void run() {
            try {
              check.invoke(guard);
            } catch (Throwable t) {
              exception[0] = t;
              if (!guard.myHasResult) guard.setResult(null);
            }
          }
        });
      }
    });
    ExceptionUtil.rethrowNullable(exception[0]);
    return result;
  }

  public void waitForCondition() throws InterruptedException {
    long start = System.currentTimeMillis();
    int iteration = 0;
    while (!myHasResult) {
      iteration++;
      long now = System.currentTimeMillis();
      if (iteration > MIN_RETRIES && !ourIgnoreTimeout.get() && now - start > TIME_OUT)
        Assert.fail("Timeout: " + (now - start) + "ms");
      myCheck.invoke(this);
      synchronized (this) {
        wait(WAIT_PERIOD);
      }
    }
  }

  public void setResult(T result) {
    synchronized (this) {
      myResult = result;
      myHasResult = true;
      notifyAll();
    }
  }

  public T waitAndGet() throws InterruptedException {
    waitForCondition();
    return getResult();
  }

  public T getResult() {
    synchronized (this) {
      if (!myHasResult) Assert.fail("No result");
      return myResult;
    }
  }
}
