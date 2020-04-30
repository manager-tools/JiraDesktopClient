package com.almworks.api.http;

public class HttpCancelledException extends HttpLoaderException {
  public HttpCancelledException() {
  }

  public HttpCancelledException(String message) {
    super(message);
  }

  public HttpCancelledException(Throwable cause) {
    super(cause);
  }
}
