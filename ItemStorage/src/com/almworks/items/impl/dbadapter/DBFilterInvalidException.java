package com.almworks.items.impl.dbadapter;

import com.almworks.items.api.DBException;

/**
 * Signals that the BoolExpr&lt;DP&gt; cannot be executed for some reason.
 */
public class DBFilterInvalidException extends DBException {
  public DBFilterInvalidException() {
  }

  public DBFilterInvalidException(String message) {
    super(message);
  }

  public DBFilterInvalidException(String message, Throwable cause) {
    super(message, cause);
  }

  public DBFilterInvalidException(Throwable cause) {
    super(cause);
  }
}
