package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;

public abstract class BottleneckJobs<T> extends AbstractJobs<T> {
  private final Bottleneck myBottleneck;

  public BottleneckJobs(long delay, ThreadGate gate) {
    myBottleneck = new Bottleneck(delay, gate, myRunnable);
  }

  protected void request(boolean delayed) {
    if (delayed)
      myBottleneck.requestDelayed();
    else
      myBottleneck.request();
  }

  protected void abortBottleneck() {
    myBottleneck.abort();
  }

  protected void gate(Runnable runnable) {
    myBottleneck.getGate().execute(runnable);
  }
}
