package com.almworks.util.exec;

public class NewThreadGate extends ThreadGate {
  public void gate(final Runnable runnable) {
    ThreadFactory.create(runnable).start();
  }

  protected Target getTarget() {
    return Target.LONG;
  }

  protected Type getType() {
    return Type.QUEUED;
  }
}
