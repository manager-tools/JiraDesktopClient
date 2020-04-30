package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;

public abstract class DoubleBottleneckJobs<T> extends AbstractJobs<T> {
  private final DoubleBottleneck myBottleneck;

  public DoubleBottleneckJobs(int requiredDelay, int minimumPeriod, ThreadGate gate) {
    myBottleneck = new DoubleBottleneck(requiredDelay, minimumPeriod, gate, myRunnable);
  }

  protected void request(boolean delayed) {
    myBottleneck.requestDelayed();
  }

  protected void gate(Runnable runnable) {
    myBottleneck.getGate().execute(runnable);
  }

  protected void abortBottleneck() {
    myBottleneck.abort();
  }
}
