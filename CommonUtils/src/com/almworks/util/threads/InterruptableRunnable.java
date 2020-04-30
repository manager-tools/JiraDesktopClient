package com.almworks.util.threads;

import org.almworks.util.RuntimeInterruptedException;

/**
 * @author dyoma
 */
public interface InterruptableRunnable {
  void run() throws InterruptedException;

  class Wrapper implements Runnable {
    private final InterruptableRunnable myRunnable;

    public Wrapper(InterruptableRunnable runnable) {
      myRunnable = runnable;
    }

    public void run() {
      try {
        myRunnable.run();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }
}
