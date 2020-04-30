package com.almworks.util.concurrent;

import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Detach;

import java.util.concurrent.CountDownLatch;

public class DetachLatch extends Detach implements Runnable {
  private final CountDownLatch myLatch = new CountDownLatch(1);

  @Override
  protected void doDetach() throws Exception {
    myLatch.countDown();
  }

  @Override
  public void run() {
    detach();
  }

  public void await() throws InterruptedException {
    myLatch.await();
  }

  public boolean awaitIgnoring() {
    try {
      myLatch.await();
      return true;
    } catch(InterruptedException e) {
      return false;
    }
  }

  public void awaitOrThrowRuntime() {
    try {
      myLatch.await();
    } catch(InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  public boolean awaitOrInterrupt() {
    try {
      myLatch.await();
      return true;
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public boolean awaitOrLogError() {
    try {
      myLatch.await();
      return true;
    } catch(InterruptedException e) {
      Log.error(e);
      return false;
    }
  }
}
