package com.almworks.util.exec;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future implementation that represents a computation. Computation may be requested to stop and sets its result when it is done.<br>
 * Result of a canceled computation is {@code null}. <br>
 * */
public class Computation<R> implements Future<R> {
  private final Object myLock = new Object();
  // guarded by myLock {
  private R myResult;
  private boolean myCancelled;
  private boolean myDone;
  private Throwable myFailure;
  // } end guarded by myLock

  public static <R> Computation<R> create() {
    return new Computation<R>();
  }

  public boolean setResult(R result) {
    synchronized (myLock) {
      if (isComplete()) return false;
      myResult = result;
      myDone = true;
      myLock.notifyAll();
    }
    return true;
  }

  private boolean isComplete() {
    return myCancelled || myDone;
  }

  public boolean fail(Throwable failure) {
    synchronized (myLock) {
      if (isComplete()) return false;
      myDone = true;
      myFailure = failure;
      myLock.notifyAll();
    }
    return true;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    synchronized (myLock) {
      if (isComplete()) return false;
      myResult = null;
      myCancelled = true;
      myLock.notifyAll();
    }
    return true;
  }

  @Override
  public boolean isCancelled() {
    synchronized (myLock) {
      return myCancelled;
    }
  }

  @Override
  public boolean isDone() {
    synchronized (myLock) {
      return myDone;
    }
  }

  @Override
  public R get() throws InterruptedException, ExecutionException {
    synchronized (myLock) {
      while (!myDone && !myCancelled) {
        myLock.wait();
      }
      if (myFailure != null) throw new ExecutionException(myFailure);
      return myResult;
    }
  }

  @Override
  public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long timeoutMs = unit.toMillis(timeout);
    long deadline = System.currentTimeMillis() + timeoutMs;
    synchronized (myLock) {
      while (!myDone && !myCancelled && System.currentTimeMillis() < deadline) {
        myLock.wait(timeoutMs);
      }
      if (myFailure != null) throw new ExecutionException(myFailure);
      return myResult;
    }
  }

  public Throwable getFailure() {
    synchronized (myLock) {
      return myFailure;
    }
  }
}
