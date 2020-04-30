package com.almworks.api.connector;

import com.almworks.api.http.HttpLoaderException;

import java.io.IOException;

public class ConnectorLoaderException extends IOException {
  private final HttpLoaderException myCause;

  public ConnectorLoaderException(HttpLoaderException cause) {
    super(cause == null ? "" : cause.getMessage());
    myCause = cause;
  }

  public HttpLoaderException getCause() {
    return myCause;
  }
}
