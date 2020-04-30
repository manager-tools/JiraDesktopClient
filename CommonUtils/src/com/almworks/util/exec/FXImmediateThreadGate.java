package com.almworks.util.exec;

import com.almworks.util.LogHelper;
import com.sun.javafx.application.PlatformImpl;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class FXImmediateThreadGate extends ImmediateThreadGate {
  private final boolean myAllowAwt;

  public FXImmediateThreadGate(boolean allowAwt) {
    myAllowAwt = allowAwt;
  }

  @Override
  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (!myAllowAwt && EventQueue.isDispatchThread()) LogHelper.error("Running from AWT thread");
    PlatformImpl.runAndWait(runnable);
  }
}
