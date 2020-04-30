package com.almworks.api.application;

public class BadItemException extends Exception {
  private final long myItem;

  public BadItemException(String message, long item) {
    super(message);
    myItem = item;
  }
}
