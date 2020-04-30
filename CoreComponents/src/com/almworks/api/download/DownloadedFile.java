package com.almworks.api.download;

import com.almworks.util.Enumerable;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.GlobalColors;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public interface DownloadedFile {
  @ThreadSafe
  String getKeyURL();

  @ThreadSafe
  State getState();

  @ThreadSafe
  ProgressSource getDownloadProgressSource();

  @ThreadSafe
  File getFile();

  @ThreadSafe
  long getSize();

  @ThreadSafe
  String getLastDownloadError();

  @ThreadSafe
  String getMimeType();

  @ThreadSafe
  long getLastModifed();

  public static final class State extends Enumerable<State> {
    public static final State UNKNOWN = new State("UNKNOWN");
    public static final State QUEUED = new State("QUEUED");
    public static final State DOWNLOADING = new State("DOWNLOADING");
    public static final State READY = new State("READY");
    public static final State DOWNLOAD_ERROR = new State("DOWNLOAD_ERROR");
    public static final State LOST = new State("LOST");

    private State(String name) {
      super(name);
    }

    public static String getStateString(State state) {
      if (state == DOWNLOAD_ERROR) {
        return "Download Failed";
      } else if (state == DOWNLOADING) {
        return "Downloading\u2026";
      } else if (state == QUEUED) {
        return "Waiting\u2026";
      } else if (state == READY) {
        return "Downloaded";
      } else {
        return "Not Downloaded";
      }
    }

    public static Color getStateColor(JComponent c, State state) {
      if (state == DOWNLOAD_ERROR) {
        return GlobalColors.ERROR_COLOR;
      } else {
        return c.getForeground();
      }
    }
  }
}
