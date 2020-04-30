package com.almworks.http;

import com.almworks.util.L;

import java.io.IOException;

public class TransferRuntimeWrapperException extends IOException {
  public TransferRuntimeWrapperException(RuntimeException e) {
    super(L.content("Failed to load data (" + e.getMessage() + ")"));
  }
}
