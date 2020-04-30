package com.almworks.util.exec;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

class AwtNonImmediateThreadGate extends ThreadGate {
  private final Type myType;

  public AwtNonImmediateThreadGate(Type type) {
    myType = type;
  }

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (myType == Type.OPTIMAL && Context.isAWT()) {
      runnable.run();
    } else {
      invokeLater(runnable);
    }
  }

  protected void invokeLater(Runnable runnable) {
    EventQueue.invokeLater(runnable);
  }

  protected final Target getTarget() {
    return Target.AWT;
  }

  protected final Type getType() {
    return myType;
  }
}
