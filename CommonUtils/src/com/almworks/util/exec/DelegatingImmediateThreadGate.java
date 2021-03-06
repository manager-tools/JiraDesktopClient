package com.almworks.util.exec;

import java.lang.reflect.InvocationTargetException;

public abstract class DelegatingImmediateThreadGate extends ImmediateThreadGate {
  protected abstract ImmediateThreadGate getDelegate();

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    ImmediateThreadGate gate = getDelegate();
    if (gate == null) {
      assert false : "no delegate for " + runnable + " (" + this + ")";
      gate = STRAIGHT;
    }
    gate.gate(runnable);
  }

  protected Target getTarget() {
    ThreadGate gate = getDelegate();
    return gate == null ? Target.STRAIGHT : gate.getTarget();
  }
}
