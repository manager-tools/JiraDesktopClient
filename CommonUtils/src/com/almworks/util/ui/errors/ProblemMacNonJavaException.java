package com.almworks.util.ui.errors;

import com.almworks.util.Env;
import org.almworks.util.Util;

public class ProblemMacNonJavaException extends KnownProblem {
  public ProblemMacNonJavaException() {
    super("mac:non-java");
  }

  protected boolean isProblem(Throwable throwable, StackTraceElement[] trace) {
    return Env.isMac() && throwable.getClass().equals(RuntimeException.class) &&
      Util.lower(Util.NN(throwable.getMessage())).indexOf("non-java exception raised") >= 0;
  }
}
