package com.almworks.api.application;

public class UnresolvedNameException extends Exception {
  public UnresolvedNameException(String id) {
    super(id);
  }
}
