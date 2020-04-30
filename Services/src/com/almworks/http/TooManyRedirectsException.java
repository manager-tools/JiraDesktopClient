package com.almworks.http;

import java.io.IOException;

public class TooManyRedirectsException extends IOException {
  public TooManyRedirectsException(int redirects) {
    super("Too many redirects (" + redirects + ")");
  }
}
