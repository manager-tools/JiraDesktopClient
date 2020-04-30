package com.almworks.api.download;

public class DownloadRequest {
  private final DownloadOwner myDownloadOwner;
  private final long myExpectedSize;
  private final String myArgument;
  private final String mySuggestedFilename;

  public DownloadRequest(DownloadOwner downloadOwner, String argument, String suggestedFilename, long expectedSize) {
    assert downloadOwner != null;
    myDownloadOwner = downloadOwner;
    myArgument = argument;
    mySuggestedFilename = suggestedFilename;
    myExpectedSize = expectedSize;
  }

  public String getArgument() {
    return myArgument;
  }

  public DownloadOwner getOwner() {
    return myDownloadOwner;
  }

  public long getExpectedSize() {
    return myExpectedSize;
  }

  public String getSuggestedFilename() {
    return mySuggestedFilename;
  }
}
