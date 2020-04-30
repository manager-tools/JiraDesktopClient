package com.almworks.util.exec;

import com.almworks.util.LogHelper;
import javafx.application.Platform;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class AwtImmediateThreadGate extends ImmediateThreadGate {
  private static boolean ourCheckFXThread = true;

  protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (Context.isAWT()) {
      runnable.run();
    } else {
      invoke(runnable);
    }
  }

  protected void invoke(Runnable runnable) throws InterruptedException, InvocationTargetException {
    if (ourCheckFXThread && Platform.isFxApplicationThread()) LogHelper.error("Running from FX thread");
    EventQueue.invokeAndWait(runnable);
  }

  protected final Target getTarget() {
    return Target.AWT;
  }

  /**
   * For test purpose only!
   * Enables tests to switch off FX thread check and run in headless environment
   * @param checkFxThread false to disable thread test
   */
  public static void testSetOurCheckFXThread(boolean checkFxThread) {
    ourCheckFXThread = checkFxThread;
  }
}
