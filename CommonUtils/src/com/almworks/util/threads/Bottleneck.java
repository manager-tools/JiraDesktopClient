package com.almworks.util.threads;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Makes sure that certain event does not occur more frequently than given period.
 */
public class Bottleneck implements Runnable, ChangeListener {
  private final Object myLock = new Object();
  private final ThreadGate myGate;
  private final Runnable myRunnable;
  private final long myMinimumPeriod;
  private final Synchronized<State> myState = new Synchronized<State>(State.IDLE, myLock);

  private Timer myTimer;
  private final ActionListener myListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      onTimer();
    }
  };

  private long myNextRunTime = 0;
  private int myPendingRequestsCount = 0;

  /**
   * @param minimumPeriod if zero, then all invocations of run()
   */
  public Bottleneck(long minimumPeriod, @NotNull ThreadGate gate, final @NotNull Runnable runnable) {
    assert minimumPeriod > 0;
    myGate = gate;
    myMinimumPeriod = minimumPeriod;
    myRunnable = new Runnable() {
      public void run() {
        doRun(runnable);
      }
    };
  }

  /**
   * Runs the runnable that was specified in the constructor of FrequentEventBottleneck.
   * If this runnable
   *
   * @deprecated use request() and requestDelayed()
   */
  @Deprecated
  public void run() {
    request();
  }


  public void onChange() {
    request();
  }

  /**
   * Requests that payload runnable gets executed at some point in time - now or later.
   * <p>
   * The contract:
   * <ul>
   * <li>Runnable is not executed more frequently than once in minimumPeriod milliseconds. More precisely,
   *     the time between previous runnable execution ends and the new execution starts is guaranteed to be no less
   *     than minimumPeriod.
   * <li>It is guaranteed that runnable will get executed at some point after request() method is entered.
   * <li>If there's no reason to wait, runnable will be executed immediately (with possible delays because of gating).
   * </ul>
   * <b>NB:</b> As a consequence of this contract, if request() is called during the execution of Runnable, then
   * another execution will start approximately minimumPeriod milliseconds after the current execution terminates.
   */
  public void request() {
    boolean runNow = false;
    synchronized (myLock) {
      State state = myState.get();
      if (state == State.IDLE) {
        long waitTime = myNextRunTime - System.currentTimeMillis();
        if (waitTime <= 0) {
          clearTimer();
          runNow = true;
          assert myPendingRequestsCount == 0;
          myPendingRequestsCount = 0;
          myState.set(State.GATING);
        } else {
          assert myPendingRequestsCount == 0;
          myPendingRequestsCount++;
          armTimer(waitTime);
          myState.set(State.WAITING);
        }
      } else {
        myPendingRequestsCount++;
      }
    }
    if (runNow)
      runInGate();
  }

  /**
   * Same as {@link #request}, but makes sure execution won't start earlier than minimumPeriod milliseoncds since calling
   * this method. If runnable is being executed now, the result is the same as calling request(), because bottleneck
   * will wait at least minimumPeriod after execution stops.
   */
  public void requestDelayed() {
    delay();
    request();
  }

  /**
   * Sets "pause" to begin from current time.
   */
  public void delay() {
    delay(0);
  }

  /**
   * Sets additional pause - next time subject will be run not earlier than after pause milliseconds.
   */
  public void delay(long pause) {
    synchronized (myLock) {
      myNextRunTime = Math.max(myNextRunTime, System.currentTimeMillis() + Math.max(pause, myMinimumPeriod));
    }
  }

  private void armTimer(long waitTime) {
    synchronized (myLock) {
      Timer timer = new Timer((int) waitTime, myListener);
      timer.setRepeats(false);
      myTimer = timer;
      timer.start();
    }
  }

  private void clearTimer() {
    synchronized (myLock) {
      Timer timer = myTimer;
      if (timer != null) {
        timer.stop();
        myTimer = null;
      }
    }
  }

  private void doRun(Runnable runnable) {
    synchronized (myLock) {
      boolean runNow = myState.commit(State.GATING, State.RUNNING);
      assert runNow : myState.get();
      if (!runNow)
        return;
    }
    try {
      runnable.run();
    } catch (Exception e) {
      Log.error(e);
    } finally {
      try {
        synchronized (myLock) {
          delay();
          if (myPendingRequestsCount > 0) {
            long waitTime = myNextRunTime - System.currentTimeMillis();
            assert waitTime > 0;
            if (waitTime > 0) {
              armTimer(waitTime);
              boolean success = myState.commit(State.RUNNING, State.WAITING);
              assert success : myState.get();
            } else {
              myState.set(State.IDLE);
            }
          } else {
            boolean success = myState.commit(State.RUNNING, State.IDLE);
            assert success : myState.get();
          }
        }
      } catch (Exception e) {
        // ignore
        Log.error(e);
      }
    }
  }

  private void onTimer() {
    boolean runNow = false;
    synchronized (myLock) {
      if (myState.get() != State.WAITING || myPendingRequestsCount <= 0)
        return;
      clearTimer();
      long waitTime = myNextRunTime - System.currentTimeMillis();
      if (waitTime <= 0) {
        runNow = true;
        myPendingRequestsCount = 0;
        myState.set(State.GATING);
      } else {
        armTimer(waitTime);
      }
    }
    if (runNow)
      runInGate();
  }

  protected void runInGate() {
    myGate.execute(myRunnable);
  }

  /**
   * Drops all pending requests, resets next run time.
   */
  public void clearBacklog() {
    synchronized (myLock) {
      myNextRunTime = 0;
      myPendingRequestsCount = 0;
      clearTimer();
      myState.commit(State.WAITING, State.IDLE);
    }
  }

  public ThreadGate getGate() {
    return myGate;
  }

  public void abort() {
    clearBacklog();
  }

  private static enum State {
    IDLE,
    WAITING,
    GATING,
    RUNNING
  }
}
