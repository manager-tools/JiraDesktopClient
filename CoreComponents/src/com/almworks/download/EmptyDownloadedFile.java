package com.almworks.download;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.progress.ProgressSource;

import java.io.File;

class EmptyDownloadedFile implements DownloadedFile {
  private final String myKeyURL;

  public EmptyDownloadedFile(String keyURL) {
    myKeyURL = keyURL;
  }

  public String getKeyURL() {
    return myKeyURL;
  }

  public ProgressSource getDownloadProgressSource() {
    return ProgressSource.STUB;
  }

  public File getFile() {
    return null;
  }

  public String getLastDownloadError() {
    return null;
  }

  public long getSize() {
    return -1;
  }

  public State getState() {
    return State.UNKNOWN;
  }

  public String getMimeType() {
    return null;
  }

  public long getLastModifed() {
    return 0;
  }
}
