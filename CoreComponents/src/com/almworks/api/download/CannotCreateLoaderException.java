package com.almworks.api.download;

public class CannotCreateLoaderException extends Exception {
  public CannotCreateLoaderException() {
  }

  public CannotCreateLoaderException(Throwable cause) {
    super(cause);
  }

  public CannotCreateLoaderException(String message) {
    super(message);
  }

  public CannotCreateLoaderException(String message, Throwable cause) {
    super(message, cause);
  }
}
