package com.almworks.image;

import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * @author dyoma
 */
public class ImageLoader implements ImageObserver {
  private final Image mySource;
  private int myWidth = -1;
  private int myHeight = -1;
  private BufferedImage myBufferedImage;
  private final SynchronizedBoolean myInProgress = new SynchronizedBoolean(true);
  private boolean myReentrantGuard = false;

  private ImageLoader(Image source) {
    mySource = source;
  }

  @Nullable
  public Image waitForDone(long timeoutMillis) throws InterruptedException {
    myInProgress.waitForValue(false, timeoutMillis);
    return myInProgress.get() ? null : myBufferedImage;
  }

  public static ImageLoader load(Image image) {
    ImageLoader loader = new ImageLoader(image);
    loader.collectDimensions();
    return loader;
  }

  private boolean collectDimensions() {
      if (myBufferedImage == null) {
        if (myWidth < 0) myWidth = mySource.getWidth(this);
        if (myHeight < 0) myHeight = mySource.getHeight(this);
        if (myWidth >= 0 && myHeight >= 0)
          myBufferedImage = new BufferedImage(myWidth, myHeight, BufferedImage.TYPE_4BYTE_ABGR);
      }
      return myBufferedImage != null;
  }

  @Override
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
    if (myReentrantGuard) return true;
    myReentrantGuard = true;
    try {
      if (!collectDimensions()) return true;
      myBufferedImage.getGraphics().drawImage(mySource, 0, 0, this);
      if ((infoflags & (ImageObserver.ALLBITS | ImageObserver.FRAMEBITS)) != 0) {
        myInProgress.set(false);
        return false;
      } else {
        return true;
      }
    } finally {
      myReentrantGuard = false;
    }
  }
}
