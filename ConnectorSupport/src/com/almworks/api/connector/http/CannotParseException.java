package com.almworks.api.connector.http;

import com.almworks.api.connector.ConnectorException;
import com.almworks.util.L;

public class CannotParseException extends ConnectorException {
  public CannotParseException(String url, String message) {
    this(url, message, null);
  }

  public CannotParseException(String url, String message, Throwable cause) {
    super(message, cause,
      L.tooltip("Cannot understand the remote server" + (cause == null ? "" : (" (" + cause.getMessage() + ")"))),
      L.tooltip("There seems to be a problem understanding a remote server web page.\n" +
      "This may be a compatibility issue, please verify that your server's version is supported.\n" +
      "\n" +
      "Details:\n" +
      url + "\n" +
      message));
  }
}
