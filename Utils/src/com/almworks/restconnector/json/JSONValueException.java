package com.almworks.restconnector.json;

public class JSONValueException extends Exception {
  public JSONValueException(String message) {
    super(message);
  }

  public JSONValueException(Throwable cause) {
    super(cause);
  }

  public JSONValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
