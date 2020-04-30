package com.almworks.util.tests;

import org.almworks.util.ExceptionUtil;
import org.almworks.util.Failure;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author : Dyoma
 */
public abstract class GUITestCase extends BaseTestCase {
  protected GUITestCase() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  public static void flushAWTQueue() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
        }
      });
    } catch (Throwable e) {
      throw new Failure(e);
    }
  }

  public static void runInAWTThread(final Runnable code) throws InvocationTargetException, InterruptedException {
    final Throwable[] ee = new Throwable[1];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        try {
          code.run();
        } catch (Throwable e) {
          ee[0] = e;
        }
      }
    });
    ExceptionUtil.rethrowNullable(ee[0]);
  }
}
