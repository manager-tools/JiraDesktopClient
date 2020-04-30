package com.almworks.items.api;

public class DBException extends RuntimeException {
  public DBException() {
  }

  public DBException(String message) {
    super(message);
  }

  public DBException(String message, Throwable cause) {
    super(message, cause);
  }

  public DBException(Throwable cause) {
    super(cause);
  }
}
