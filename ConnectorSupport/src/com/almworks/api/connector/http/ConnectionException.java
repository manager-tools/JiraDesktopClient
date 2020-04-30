package com.almworks.api.connector.http;

import com.almworks.api.connector.ConnectorException;
import com.almworks.util.L;
import org.apache.commons.httpclient.HttpException;

public class ConnectionException extends ConnectorException {
  private final String myUrl;

  public ConnectionException(String url, String message) {
    this(url, message, null);
  }

  public ConnectionException(String url, String message, Throwable cause) {
    this(url, message, cause, createShortDescription(cause));
  }

  private static String createShortDescription(Throwable cause) {
    StringBuffer buf = new StringBuffer("Connection problem");
    if (cause != null) {
      // HttpException is not informative
      if (cause instanceof HttpException) {
        Throwable parent = cause.getCause();
        if (parent != null) {
          cause = parent;
        }
      }

      String message = cause.getMessage();
      if (message == null || message.length() == 0) {
        message = cause.toString();
      }
      if (message != null && message.length() > 0) {
        buf.append(": ");
        buf.append(message);
      }
    }
    return buf.toString();
  }

  public ConnectionException(String url, String message, Throwable cause, String shortDescription) {
    super(message, cause, shortDescription, L.tooltip(
      "There was a problem connecting to the remote server and reading a web page.\n" +
        "Please verify that you are online and you can reach the remote server through web browser.\n" + "\n" +
        "Details:\n" + url + "\n" + message + "\n" + (cause == null ? "" : cause.toString())));
    myUrl = url;
  }

  public String getMediumDescription() {
    return getShortDescription() + " (" + myUrl + ")";
  }
}
