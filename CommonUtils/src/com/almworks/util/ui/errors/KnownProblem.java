package com.almworks.util.ui.errors;

import org.almworks.util.Log;

import java.lang.reflect.InvocationTargetException;

public abstract class KnownProblem {
  private static final long MINIMUM_REPORTING_PERIOD = 5000;

  private final String myDescription;

  private long myLastTimeReported;
  private int myCount;

  protected KnownProblem(String description) {
    myDescription = description;
  }

  public final boolean handle(Throwable throwable, StackTraceElement[] trace) {
    try {
      while (throwable instanceof InvocationTargetException) {
        Throwable cause = throwable.getCause();
        if (cause == null)
          break;
        throwable = cause;
      }
      if (isProblem(throwable, trace)) {
        boolean reported = reportProblem();
        handleProblem(throwable, trace, reported);
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * @return true if this is the instance of the problem
   */
  protected abstract boolean isProblem(Throwable throwable, StackTraceElement[] trace);

  /**
   * Additional handling of the problem
   */
  protected void handleProblem(Throwable throwable, StackTraceElement[] trace, boolean reported) {
  }

  private synchronized boolean reportProblem() {
    long now = System.currentTimeMillis();
    myCount++;
    boolean reporting = now > myLastTimeReported + MINIMUM_REPORTING_PERIOD;
    if (reporting) {
      myLastTimeReported = now;
      Log.warn("known problem happened: " + myDescription + " (" + myCount + ")");
    }
    return reporting;
  }

  protected static boolean checkException(Throwable throwable, StackTraceElement[] trace, Class exceptionClass,
    String[] traceTop)
  {
    if (!exceptionClass.isInstance(throwable))
      return false;
    assert traceTop.length % 2 == 0 : traceTop;
    if (trace.length < (traceTop.length / 2 + 1))
      return false;
    for (int i = 0; i < traceTop.length - 1; i += 2) {
      String traceClass = traceTop[i];
      String traceMethod = traceTop[i + 1];
      StackTraceElement t = trace[i / 2];
      if (traceClass != null && !traceClass.equals(t.getClassName()))
        return false;
      if (traceMethod != null && !traceMethod.equals(t.getMethodName()))
        return false;
    }
    return true;
  }
}
