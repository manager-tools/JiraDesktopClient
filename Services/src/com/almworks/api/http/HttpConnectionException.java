package com.almworks.api.http;


public class HttpConnectionException extends HttpLoaderException {
  private final int myStatusCode;
  private final String myStatusText;

  public HttpConnectionException(String url, int statusCode, String statusText) {
    super("server responded [" + statusCode + " " + statusText + "]");
    myStatusCode = statusCode;
    myStatusText = statusText;
  }

  public int getStatusCode() {
    return myStatusCode;
  }

  public String getStatusText() {
    return myStatusText;
  }
}
