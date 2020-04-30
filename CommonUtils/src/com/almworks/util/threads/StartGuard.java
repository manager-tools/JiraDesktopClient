package com.almworks.util.threads;

import org.almworks.util.Log;

/**
 * Provides guard for concurrent startup activity. This guard ensures that only one thread can pass the guard to perform
 * startup activity and if the startup succeeds no other threads are required to perform it again. The guard supports
 * startup failures: FATAL - means no farther activity can succeed, RESTARTABLE - this startup failed but next attempt may
 * succeed.<br>
 * Usage:<br>
 * <code>
 * void ensureStarted() {<br>
 *    if (!myStartupGuard.{@link #tryStartup()}) return;<br>
 *    try {<br>
 *      doStart(); // perform start up, throws an exception if failed. <br>
 *      guard.succeeded(); // If this line is reached startup is marked successfuly completed. All other threads are allowed to bypass startup<br>
 *    } finally {<br>
 *      guard.{@link #activityFinished()}; // Notifies that startup activity is finished. If startup is already marked as success or fatal failure<br>
 *                                // does nothing, otherwise allows other threads to perform retry startup <br>
 *    }<br>
 * }
 * </code>
 */
public class StartGuard {
  private static final int STATE_NEVER_STARTED = 0;
  private static final int STATE_START_DONE = 1;
  private static final int STATE_START_FATAL_FAILED = 2;
  private static final int STATE_START_FAILED_RESTARTABLE = 3;

  private int myState = STATE_NEVER_STARTED;
  private Thread myStarter = null;

  /**
   * Checks if startup activity is required and possible.<br>
   * During startup activity caller should call {@link #succeeded()} or {@link #fatalFailure()} to notify successful completion,
   * or fatal failure. If none of these is called the startup activity is marked as not fatally failed. This allows another
   * thread to retry it.
   * @return true means that caller thread is allowed to perform startup. The caller thread must call {@link #activityFinished()}.<br>
   * false means that startup activity is already performed or has fatally failed. Caller need not perform startup.
   * @throws InterruptedException
   */
  public boolean tryStartup() throws InterruptedException {
    synchronized (this) {
      if (isFinalState()) return false;
      while (myStarter != null) {
        wait();
        if (isFinalState()) return false;
      }
      myStarter = Thread.currentThread();
      return true;
    }
  }

  /**
   * Starter notifies this guard that startup activity is finished. Calling this method allows other threads to retry
   * startup activity if it not fatally failed.
   */
  public void activityFinished() {
    synchronized (this) {
      if (!checkThread()) return;
      myStarter = null;
      if (!isFinalState()) myState = STATE_START_FAILED_RESTARTABLE;
      notifyAll();
    }
  }

  /**
   * Starter notifies guard that startup activity is already successfully completed. This unlocks all threads waiting for
   * startup to complete.
   */
  public void succeeded() {
    synchronized (this) {
      if (!checkThread()) return;
      if (isFinalState()) return;
      myState = STATE_START_DONE;
      notifyAll();
    }
  }

  /**
   * Starter notifies guard that startup activity is already fatally failed. This unlocks all threads waiting for
   * startup to complete.
   */
  public void fatalFailure() {
    synchronized (this) {
      if (!checkThread()) return;
      if (isFinalState()) return;
      myState = STATE_START_FATAL_FAILED;
      notifyAll();
    }
  }

  private boolean checkThread() {
    assert Thread.holdsLock(this);
    Thread thisThread = Thread.currentThread();
    if (myStarter != thisThread) {
      Log.error("Wrong thread. Expected: " + myStarter + " but called from: " + thisThread);
      return false;
    }
    return true;
  }

  private boolean isFinalState() {
    assert Thread.holdsLock(this);
    return myState == STATE_START_DONE || myState == STATE_START_FATAL_FAILED;
  }
}
