package com.almworks.util.ui.errors;

import com.almworks.util.Env;
import org.almworks.util.Log;

public class ProblemJavaLibraryException extends KnownProblem {
  public ProblemJavaLibraryException() {
    super("pure_java_exception");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    if (Env.isDebugging() || Env.isRunFromIDE()) {
      // should know
      return false;
    }
    boolean assertions = false;
    assert assertions = true;
    if (assertions) {
      // should know
      return false;
    }
    for (StackTraceElement element : trace) {
      String method = element.getMethodName();
      if ("dispatchEvent".equals(method)) {
        // all on top of dispatch event are event handlers - not our error
        return true;
      }
      String className = element.getClassName();
      if (className.indexOf(".almworks.") >= 0 || className.startsWith("z.")) {
        if ("dispatchEvent".equals(method) || "paintChildren".equals(method) || "paint".equals(method)) {
          // event queue replacement
          // painting
          continue;
        }
        // have alm works in trace
        return false;
      }
    }
    return true;
  }

  protected void handleProblem(Throwable throwable, StackTraceElement[] trace, boolean reported) {
    if (reported) {
      Log.warn(throwable);
    }
  }
}
