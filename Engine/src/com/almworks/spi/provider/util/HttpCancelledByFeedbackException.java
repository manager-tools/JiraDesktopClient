package com.almworks.spi.provider.util;

import com.almworks.api.http.HttpCancelledException;

public class HttpCancelledByFeedbackException extends HttpCancelledException {
  public HttpCancelledByFeedbackException(String message) {
    super(message);
  }
}
