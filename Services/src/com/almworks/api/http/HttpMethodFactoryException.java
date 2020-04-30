package com.almworks.api.http;

public class HttpMethodFactoryException extends HttpLoaderException {
  public HttpMethodFactoryException(String message) {
    super(message);
  }

  public HttpMethodFactoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
