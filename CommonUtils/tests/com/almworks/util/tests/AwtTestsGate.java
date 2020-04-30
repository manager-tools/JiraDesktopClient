package com.almworks.util.tests;

import com.almworks.util.exec.AwtImmediateThreadGate;
import com.almworks.util.exec.ImmediateThreadGate;
import org.almworks.util.RuntimeInterruptedException;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class AwtTestsGate extends AwtImmediateThreadGate {
  public static final ImmediateThreadGate AWT_FOR_TEST = new AwtTestsGate();

  protected void invoke(Runnable runnable) throws InterruptedException, InvocationTargetException {
    // set thread name to AWT-...
    // see BaseTestCase#isGuiThread
    Thread thread = Thread.currentThread();
    String savedName = thread.getName();
    try {
      thread.setName("AWT-SYNC-" + savedName);
    } catch (Exception e) {
      // ignore
    }
    try {
      SwingUtilities.invokeAndWait(runnable);
    } finally {
      try {
        thread.setName(savedName);
      } catch (Exception e) {
        // ignore
      }
    }
  }

  protected void onException(Throwable e) {
    if (!(e instanceof InterruptedException) && !(e instanceof RuntimeInterruptedException)) {
      super.onException(e);
    } else {
      // ignore
    }
  }
}
