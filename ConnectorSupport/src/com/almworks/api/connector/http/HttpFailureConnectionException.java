package com.almworks.api.connector.http;

import com.almworks.api.http.HttpConnectionException;
import com.almworks.util.L;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class HttpFailureConnectionException extends ConnectionException {
  private final int myStatusCode;
  private final String myStatusText;

  public HttpFailureConnectionException(String url, int statusCode, String statusText) {
    super(url,
      url + " responded [" + statusCode + " " + statusText + "]",
      null,
      L.tooltip("Server HTTP response [" + statusCode + " " + statusText + "]"));
    myStatusCode = statusCode;
    myStatusText = statusText;
  }

  public int getStatusCode() {
    return myStatusCode;
  }

  public String getStatusText() {
    return myStatusText;
  }

  /**
   * Searches the exception and it's {@link Throwable#getCause() causes} for an exception with HTTP status code.
   * @return http status code or 0 if no http code found
   */
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static int findHttpCode(@Nullable Throwable e) {
    while (e != null) {
      HttpConnectionException httpException = Util.castNullable(HttpConnectionException.class, e);
      if (httpException != null) return httpException.getStatusCode();
      HttpFailureConnectionException failure = Util.castNullable(HttpFailureConnectionException.class, e);
      if (failure != null) return failure.getStatusCode();
      e = e.getCause();
    }
    return 0;
  }
}
