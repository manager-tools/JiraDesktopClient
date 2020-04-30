package com.almworks.api.http;

public class HttpLoaderException extends Exception {
  public HttpLoaderException() {
  }

  public HttpLoaderException(Throwable cause) {
    super(cause);
  }

  public HttpLoaderException(String message) {
    super(message);
  }

  public HttpLoaderException(String message, Throwable cause) {
    super(message, cause);
  }
}
