package com.almworks.util.exec;

import javafx.application.Platform;

import java.lang.reflect.InvocationTargetException;

public class FXNotImmediateThreadGate extends ThreadGate {
  private final Type myType;

  public FXNotImmediateThreadGate(Type type) {
    myType = type;
  }

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (myType == Type.OPTIMAL && Platform.isFxApplicationThread()) {
      runnable.run();
    } else {
      invokeLater(runnable);
    }
  }

  protected void invokeLater(Runnable runnable) {
    Platform.runLater(runnable);
  }

  protected final Target getTarget() {
    return Target.FX;
  }

  protected final Type getType() {
    return myType;
  }
}
