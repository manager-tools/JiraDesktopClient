package com.almworks.util.ui.errors;

import com.almworks.util.Env;
import org.almworks.util.Log;

public class ProblemFuckingLameSwabungaLibrary extends KnownProblem {
  public ProblemFuckingLameSwabungaLibrary() {
    super("PFLSL");
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
//      return false;
    }
    for (StackTraceElement element : trace) {
      String className = element.getClassName();
      if (className.contains(".swabunga."))
        return true;
    }
    return false;
  }

  protected void handleProblem(Throwable throwable, StackTraceElement[] trace, boolean reported) {
    if (reported) {
      Log.warn(throwable);
    }
  }
}
