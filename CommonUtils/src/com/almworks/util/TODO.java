package com.almworks.util;

import org.almworks.util.Failure;

/**
 * These are marker methods for code, to be used instead of simple todos.
 * You may use them like "assert TODO.todo();"
 *
 * @author sereda
 */
public class TODO {
  public static final boolean RETURN_VALUE = true;

  public static boolean boundaryCondition() {
    return RETURN_VALUE;
  }

  public static boolean exceptionHandling() {
    return RETURN_VALUE;
  }

  public static boolean functional() {
    return RETURN_VALUE;
  }

  public static Failure failure() {
    return new Failure("todo");
  }

  public static RuntimeException notImplementedYet() {
    throw new RuntimeException("Not implemented yet");
  }

  public static RuntimeException notImplementedYet(String hint) {
    throw new RuntimeException(hint);
  }

  public static RuntimeException shouldNotHappen(String hint) {
    throw new Failure(hint);
  }
}
