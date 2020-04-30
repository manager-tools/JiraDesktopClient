package org.almworks.util;

/**
 * This exception is used where an InterruptedException should be propagated with a runtime exception.
 * The constructor interrupts current thread so the interrupted flag is not lost.
 */
public class RuntimeInterruptedException extends RuntimeException {
  public RuntimeInterruptedException(InterruptedException cause) {
    super(cause);
    Thread.currentThread().interrupt();
  }
}
