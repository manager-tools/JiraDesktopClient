package com.almworks.util.exec;

import java.lang.reflect.InvocationTargetException;

class StraightGate extends ImmediateThreadGate {
  public static final StraightGate INSTANCE = new StraightGate();

  private StraightGate() {
  }

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    runnable.run();
  }

  protected Target getTarget() {
    return Target.STRAIGHT;
  }
}
