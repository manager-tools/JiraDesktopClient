package com.almworks.util.threads;

import com.almworks.util.exec.Context;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * :todoc:
 * :todo: component and abstraction
 *
 * @author sereda
 */
public class Threads {
  public static Thread createThread(String name, Runnable runnable) {
    return new Thread(runnable, name);
  }

  public static void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(runnable);
  }

  public static <T> T computeInAWTThread(final Computable<T> computable) throws InvocationTargetException,
    InterruptedException {
    if (SwingUtilities.isEventDispatchThread())
      return computable.compute();
    final T[] value = (T[]) new Object[1];
    final Throwable[] exception = new Throwable[1];
    invokeAndWait(new Runnable() {
      public void run() {
        try {
          value[0] = computable.compute();
        } catch (Throwable e) {
          exception[0] = e;
        }
      }
    });
    if (exception[0] == null)
      return value[0];
    throw ExceptionUtil.rethrow(exception[0]);
  }

  // todo :refactoring: assert not only that we're not in AWT thread, but that we're not inside transaction
  public static void assertLongOperationsAllowed() {
    assert doAssertLongOperationAllowed();
  }

  private static boolean doAssertLongOperationAllowed() {
    if (!Context.isAWT())
      return true;
    if (isDebuggerEvaluating())
      return true;
    assert false : "running long operation in UI thread";
    Log.warn("running long operation in UI thread", new IllegalStateException());
    return false;
  }

  private static boolean isDebuggerEvaluating() {
    StackTraceElement[] trace = new Exception().getStackTrace();
    for (StackTraceElement element : trace) {
      if (element.getClassName().equals("com.intellij.rt.debugger.BatchEvaluatorServer"))
        return true;
    }
    return false;
  }

  public static void assertAWTThread() {
    assert doAssertAWTThread();
  }

  private static boolean doAssertAWTThread() {
    if (Context.isAWT()) return true;
    if (SwingUtilities.isEventDispatchThread()) {
      Context.clearIsAWT(); // AWT thread change detected
      return true;
    }
    if (isDebuggerEvaluating()) return true;
    assert false : "running not in AWT thread (" + Thread.currentThread() + ")";
    Log.warn("running UI operation not in UI thread " + Thread.currentThread(), new IllegalStateException());
    return true;
  }

  public static void assertIndefiniteWaitAllowed() {
    assertLongOperationsAllowed();
  }
}
