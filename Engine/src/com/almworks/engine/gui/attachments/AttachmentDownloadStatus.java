package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadedFile;
import com.almworks.api.download.FileDownloadListener;
import com.almworks.api.install.TrackerProperties;
import com.almworks.util.Env;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.progress.ProgressSource;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentDownloadStatus<T extends Attachment> {
  private static final int DEFAULT_AUTO_DOWNLOAD_MAXSIZE = 102400;
  private final Map<String, DownloadedFile> myStatusMap =
    Collections.synchronizedMap(new HashMap<String, DownloadedFile>());
  private final Map<String, Detach> myDownloadMap = Collections15.hashMap();
  private final FileDownloadListener myDownloadCallback;
  // is fired on each download event
  private final SimpleModifiable myDownloadModifiable = new SimpleModifiable();
  private DownloadManager myDownloadManager;

  public AttachmentDownloadStatus(FileDownloadListener downloadCallback) {
    myDownloadCallback = downloadCallback;
  }

  public void watch(final Lifespan life, final AListModel<T> model, DownloadManager downloadManager) {
    myDownloadManager = downloadManager;
    life.add(new Detach() {
      protected void doDetach() {
        for (Detach detach : myDownloadMap.values()) detach.detach();
        myDownloadMap.clear();
        myStatusMap.clear();
      }
    });
    DownloadModelListener listener = new DownloadModelListener(life, model);
    listener.start();
  }

  private long getAutoDownloadThreshold() {
    return Env.getInteger(TrackerProperties.AUTO_DOWNLOAD_MAXSIZE, DEFAULT_AUTO_DOWNLOAD_MAXSIZE);
  }

  public DownloadedFile getDownloadedFile(String url) {
    return url != null ? myStatusMap.get(url) : null;
  }

  public Modifiable getModifiable() {
    return myDownloadModifiable;
  }

  public DownloadManager getDownloadManager() {
    return myDownloadManager;
  }

  private class DownloadModelListener extends AListModel.Adapter implements FileDownloadListener {
    private final Lifespan myLife;
    private final AListModel<T> myModel;

    public DownloadModelListener(Lifespan life, AListModel<T> model) {
      myLife = life;
      myModel = model;
    }

    public void onInsert(int index, int length) {
      watchAttachments(myLife, myModel, index, length);
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
      List<T> removed = event.getAllRemoved();
      DownloadManager downloadManager = Context.require(DownloadManager.ROLE);
      for (T item : removed) {
        String url = item.getUrl();
        if (url != null) {
          downloadManager.removeFileDownloadListener(url, this);
        }
      }
    }

    private Detach trackDownloadProgress(final String url, final DownloadedFile dFile) {
      Detach downloadDetach;
      final ProgressSource source = dFile.getDownloadProgressSource();
      downloadDetach = source.getModifiable().addAWTChangeListener(new ChangeListener() {
        double myLastProgress = -1;

        public void onChange() {
          double progress = source.getProgress();
          if (progress - myLastProgress > 0.01F) {
            myLastProgress = progress;
            myDownloadCallback.onDownloadStatus(dFile);
          }
        }
      });
      return downloadDetach;
    }

    public void onDownloadStatus(DownloadedFile dfile) {
      String url = dfile.getKeyURL();
      if (url == null) {
        assert false : dfile;
        return;
      }
      myStatusMap.put(url, dfile);

      Detach downloadDetach = myDownloadMap.get(url);
      DownloadedFile.State state = dfile.getState();
      if (state == DownloadedFile.State.DOWNLOADING) {
        if (downloadDetach == null) {
          downloadDetach = trackDownloadProgress(url, dfile);
          myDownloadMap.put(url, downloadDetach);
        }
      } else {
        if (downloadDetach != null) {
          downloadDetach.detach();
          myDownloadMap.remove(url);
        }
      }
      myDownloadCallback.onDownloadStatus(dfile);
      myDownloadModifiable.fireChanged();
    }

    private void watchAttachments(Lifespan life, AListModel<T> model, int index, int length) {
      DownloadManager downloadManager = Context.require(DownloadManager.ROLE);
      long threshold = getAutoDownloadThreshold();
      for (int i = 0; i < length; i++) {
        T item = model.getAt(index + i);
        String url = item.getUrl();
        if (url != null) {
          downloadManager.checkDownloadedFile(url);
          long expectedSize = item.getExpectedSize();
          if (expectedSize > 0 && expectedSize <= threshold) {
            // initiate auto download
            DownloadedFile.State state = downloadManager.getDownloadStatus(url).getState();
            if (state == DownloadedFile.State.UNKNOWN || state == DownloadedFile.State.LOST) {
              downloadManager.initiateDownload(url, item.createDownloadRequest(), false, false);
            }
          }
          life.add(downloadManager.addFileDownloadListener(url, ThreadGate.AWT, this));
        }
      }
    }

    public void start() {
      myLife.add(myModel.addListener(this));
      watchAttachments(myLife, myModel, 0, myModel.getSize());
    }
  }
}
