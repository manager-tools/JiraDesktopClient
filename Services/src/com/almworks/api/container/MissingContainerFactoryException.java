package com.almworks.api.container;

public class MissingContainerFactoryException extends Exception {
  public MissingContainerFactoryException() {
  }

  public MissingContainerFactoryException(String message) {
    super(message);
  }

  public MissingContainerFactoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingContainerFactoryException(Throwable cause) {
    super(cause);
  }
}
