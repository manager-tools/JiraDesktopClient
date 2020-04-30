package com.almworks.api.tray;

public class CompatibilityException extends Exception {
  public CompatibilityException() {
  }

  public CompatibilityException(String message) {
    super(message);
  }

  public CompatibilityException(String message, Throwable cause) {
    super(message, cause);
  }

  public CompatibilityException(Throwable cause) {
    super(cause);
  }
}
