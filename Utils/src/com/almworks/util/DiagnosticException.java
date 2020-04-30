package com.almworks.util;

/**
 * A wrapper to mark exceptions that are not suppressed in
 * production versions.
 */
public class DiagnosticException extends RuntimeException {
  public DiagnosticException() {
  }

  public DiagnosticException(Throwable cause) {
    super(cause);
  }

  public DiagnosticException(String message) {
    super(message);
  }

  public DiagnosticException(String message, Throwable cause) {
    super(message, cause);
  }
}
