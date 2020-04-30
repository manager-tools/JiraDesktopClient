package com.almworks.util;

/**
 * A checked exception that doesn't collect its stack trace upon creation.
 * They are much cheaper to construct when you need an exception for control
 * flow, like {@link ContinueOrBreak} or {@link NonLocalReturn}.
 */
public class CheapCheckedException extends Exception {
  public CheapCheckedException() {}

  public CheapCheckedException(Throwable cause) {
    super(cause);
  }

  public CheapCheckedException(String message) {
    super(message);
  }

  public CheapCheckedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public Throwable fillInStackTrace() {
    return this;
  }
}
