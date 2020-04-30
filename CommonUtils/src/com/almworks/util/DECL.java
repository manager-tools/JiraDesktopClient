package com.almworks.util;

/**
 * :todoc:
 *
 * @author sereda
 */
public class DECL {
  public static boolean assumeThreadMayBeAWT() {
    return true;
  }

  /**
   * Declares that the code follows that is intended for error recovery.
   */
  public static boolean robustnessCode() {
    return true;
  }

  public static boolean nullable(Object object) {
    return true;
  }

  public static boolean assumeNonAWTThread() {
    return true;
  }

  public static void fallThrough() {
  }

  public static void ignoreException() {
  }
}
