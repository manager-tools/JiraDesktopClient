package org.almworks.util;

/**
 * This runtime error is dedicated to signal any kind of (critical)
 * failures that happen in any class. It is presumed that application cannot
 * recover from a failure.
 *
 * @author sereda
 */
public class Failure extends RuntimeException {
  public Failure() {
    super();
  }

  public Failure(String message) {
    super(message);
  }

  public Failure(Throwable cause) {
    super(cause);
  }

  public Failure(String message, Throwable cause) {
    super(message, cause);
  }
}
