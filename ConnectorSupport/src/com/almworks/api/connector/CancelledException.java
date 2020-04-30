package com.almworks.api.connector;

import com.almworks.util.L;

public class CancelledException extends ConnectorException {
  public CancelledException() {
    super("synchronization cancelled", L.tooltip("Cancelled"),
      L.tooltip("Synchronization was cancelled by user request"));
  }

  public CancelledException(Throwable e) {
    super("synchronization cancelled", e, L.tooltip("Cancelled"), L.tooltip("Synchronization was cancelled"));
  }
}
