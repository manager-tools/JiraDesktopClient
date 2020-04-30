package com.almworks.util.exec;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadFuture<T> implements Future<T> {
  private final Thread myThread;
  private final Computation<T> myComputation = new Computation<T>();

  public ThreadFuture(Thread thread) {
    myThread = thread;
  }

  public boolean setResult(T result) {
    return myComputation.setResult(result);
  }

  public boolean fail(Throwable throwable) {
    return myComputation.fail(throwable);
  }

  public boolean checkCancelled() throws InterruptedException {
    boolean cancelled = isCancelled();
    if (cancelled && myThread == Thread.currentThread()) {
      myThread.interrupt();
      throw new InterruptedException("Cancelled");
    }
    return cancelled;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!mayInterruptIfRunning) return false;
    boolean cancel = myComputation.cancel(mayInterruptIfRunning);
    if (cancel) myThread.interrupt();
    return cancel;
  }

  @Override
  public boolean isCancelled() {
    return myComputation.isCancelled();
  }

  @Override
  public boolean isDone() {
    return myComputation.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return myComputation.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return myComputation.get(timeout, unit);
  }
}
