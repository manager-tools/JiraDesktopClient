package com.almworks.image;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.File;
import java.lang.ref.WeakReference;

public class RemoteIconImpl implements Icon, ImageObserver {
  private static final long MIN_PAUSE_BETWEEN_DOWNLOAD_ATTEMPTS = 30*Const.SECOND;
  static final int DEFAULT_WIDTH = 16;
  static final int DEFAULT_HEIGHT = 16;

  private final String myUrl;

  private boolean myDownloading = false;
  private long myLastDownloadAttempt = 0;
  private String myLastError = null;

  private Image myImage;
  private File myFile;
  private String myMimeType;

  private java.util.List<WeakReference<Component>> myPendingPainters = null;

  private static final BottleneckJobs<RemoteIconImpl> ourRepaintJobs =
    new BottleneckJobs<RemoteIconImpl>(500, ThreadGate.AWT) {
      protected void execute(RemoteIconImpl job) {
        job.repaintPendingPainters();
      }
    };

  RemoteIconImpl(String url) {
    myUrl = url;
  }

  RemoteIconImpl(String url, File file, String mimeType, Image image) {
    myUrl = url;
    myFile = file;
    myMimeType = mimeType;
    myImage = image;
  }

  public synchronized boolean markForDownload() {
    if (myImage != null)
      return false;
    if (myDownloading)
      return false;
    if (myLastError != null && System.currentTimeMillis() < myLastDownloadAttempt + MIN_PAUSE_BETWEEN_DOWNLOAD_ATTEMPTS)
      return false;
    myDownloading = true;
    return true;
  }

  public String getUrl() {
    return myUrl;
  }

  public synchronized void setBroken(String message) {
    assert myDownloading : this;
    myDownloading = false;
    myLastError = message;
    myLastDownloadAttempt = System.currentTimeMillis();
    ourRepaintJobs.addJob(this);
  }

  public String toString() {
    return "RII[" + myUrl + "]";
  }

  public synchronized void setData(File file, String mimeType, Image image) {
    assert myDownloading : this;
    myDownloading = false;
    myLastError = null;
    myLastDownloadAttempt = System.currentTimeMillis();
    myFile = file;
    myMimeType = mimeType;
    myImage = image;
    ourRepaintJobs.addJob(this);
  }


  private synchronized Image getImage() {
    return myImage;
  }

  @Nullable
  public synchronized File getFile() {
    return myFile;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    Image image = getImage();
    if (image != null) {
      boolean painted = g.drawImage(image, x, y, this);
      if (!painted) {
        addPendingPainter(c);
      }
    } else {
      addPendingPainter(c);
    }
  }

  @ThreadAWT
  private void addPendingPainter(Component component) {
    // let's see if we are in a renderer
    for (Component c = component; c != null; c = c.getParent()) {
      if (c instanceof CellRendererPane) {
        component = c.getParent();
        // no break here - get the topmost owner of a CellRendererPane
      }
    }

    if (myPendingPainters == null)
      myPendingPainters = Collections15.arrayList();
    myPendingPainters.add(new WeakReference<Component>(component));
  }

  @ThreadAWT
  private void repaintPendingPainters() {
    java.util.List<WeakReference<Component>> painters = myPendingPainters;
    myPendingPainters = null;
    if (painters != null) {
      for (WeakReference<Component> ref : painters) {
        Component painter = ref.get();
        if (painter != null) {
          if (painter.isShowing()) {
            painter.repaint();
          }
        }
      }
      // help gc
      painters.clear();
    }
  }

  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
    // unknown thread!
    if ((infoflags & ALLBITS) > 0) {
      ourRepaintJobs.addJob(this);
    }
    return (infoflags & (ALLBITS | ERROR | ABORT)) == 0;
  }

  public int getIconWidth() {
    Image image = getImage();
    if (image != null) {
      int w = image.getWidth(this);
      if (w > 0)
        return w;
    }
    return DEFAULT_WIDTH;
  }

  public int getIconHeight() {
    Image image = getImage();
    if (image != null) {
      int h = image.getHeight(this);
      if (h > 0)
        return h;
    }
    return DEFAULT_HEIGHT;
  }

  public String getMimeType() {
    return myMimeType;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    return myUrl.equals(((RemoteIconImpl) o).myUrl);
  }

  public int hashCode() {
    return myUrl.hashCode();
  }
}
