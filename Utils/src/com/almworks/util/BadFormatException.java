package com.almworks.util;

/**
 * @author : Dyoma
 */
public class BadFormatException extends Exception {
  public BadFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadFormatException(String message) {
    super(message);
  }

  public BadFormatException() {
    super();
  }
}
