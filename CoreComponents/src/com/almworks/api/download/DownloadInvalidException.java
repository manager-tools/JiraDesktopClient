package com.almworks.api.download;

public class DownloadInvalidException extends Exception {
  private final boolean myShouldRetry;

  public DownloadInvalidException(String message, boolean shouldRetry) {
    super(message);
    myShouldRetry = shouldRetry;
  }

  public boolean shouldRetry() {
    return myShouldRetry;
  }
}
