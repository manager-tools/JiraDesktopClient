package com.almworks.util.threads;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;

/**
 * Double-bottleneck pattern. First bottleneck makes requests wait for some time (usually small).
 * Second bottlenect ensures that operation is not carried out too frequently.
 */
public class DoubleBottleneck implements Runnable, ChangeListener {
  private final Bottleneck myFirstBottleneck;
  private final Bottleneck mySecondBottleneck;

  /**
   * @param requiredDelay minimum time period that should pass from the time {@link #requestDelayed()} is called
   * and to the time when runnable is called.
   *
   * @param minimumPeriod minimum time period that should pass between the executions of runnable.
   *
   * @param gate gate which to use to execute runnable
   *
   * @param runnable procedure to execute
   */
  public DoubleBottleneck(int requiredDelay, int minimumPeriod, ThreadGate gate, Runnable runnable) {
    mySecondBottleneck = new Bottleneck(minimumPeriod, gate, runnable);
    myFirstBottleneck = new Bottleneck(requiredDelay, ThreadGate.STRAIGHT, mySecondBottleneck);
  }

  /**
   * DoubleBottleneck allows only delayed requests.
   */
  public void requestDelayed() {
    myFirstBottleneck.requestDelayed();
  }

  public void abort() {
    myFirstBottleneck.abort();
    mySecondBottleneck.abort();
  }

  @Deprecated
  public void run() {
    requestDelayed();
  }

  public void onChange() {
    requestDelayed();
  }

  public ThreadGate getGate() {
    return mySecondBottleneck.getGate();
  }
}
