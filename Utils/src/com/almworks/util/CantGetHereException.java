package com.almworks.util;

import org.almworks.util.Failure;

/**
 * :todoc:
 *
 * @author sereda
 */
public class CantGetHereException extends Failure {
  public CantGetHereException() {
    super();
  }

  public CantGetHereException(String message) {
    super(message);
  }

  public CantGetHereException(Throwable cause) {
    super(cause);
  }

  public CantGetHereException(String message, Throwable cause) {
    super(message, cause);
  }
}
