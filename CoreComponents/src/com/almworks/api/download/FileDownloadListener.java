package com.almworks.api.download;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.DetachComposite;

import static com.almworks.api.download.DownloadedFile.State.DOWNLOAD_ERROR;
import static com.almworks.api.download.DownloadedFile.State.READY;

public interface FileDownloadListener {
  void onDownloadStatus(DownloadedFile status);

  class Tracker implements FileDownloadListener {
    private final DetachComposite myDownloadLife = new DetachComposite();
    private final FileDownloadListener myListener;

    public Tracker(FileDownloadListener listener) {
      myListener = listener;
    }

    public void onDownloadStatus(DownloadedFile dfile) {
      DownloadedFile.State state = dfile.getState();
      if (state == DOWNLOAD_ERROR || state == READY) myDownloadLife.detach();
      myListener.onDownloadStatus(dfile);
    }

    public static void perform(DownloadManager manager, String url, ThreadGate gate, FileDownloadListener listener) {
      Tracker loaded = new Tracker(listener);
      loaded.listen(manager, url, gate);
    }

    private void listen(DownloadManager manager, String url, ThreadGate gate) {
      myDownloadLife.add(manager.addFileDownloadListener(url, ThreadGate.AWT, this));
    }
  }
}
