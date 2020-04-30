package com.almworks.util.exec;

import java.lang.reflect.InvocationTargetException;

public abstract class DelegatingThreadGate extends ThreadGate {
  protected abstract ThreadGate getDelegate();

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    ThreadGate gate = getDelegate();
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

  protected Type getType() {
    ThreadGate gate = getDelegate();
    return gate == null ? Type.IMMEDIATE : gate.getType();
  }
}
