package com.almworks.image;

import com.almworks.api.image.ThumbnailReadyNotificator;
import com.almworks.api.image.ThumbnailSourceFactory;
import com.almworks.api.image.Thumbnailer;
import com.almworks.util.LogHelper;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThumbnailerImpl implements Thumbnailer {
  public static final int CACHE_SIZE = 20;

  private final LinkedHashMap<String, CachedImage> myCache =
    new LinkedHashMap<String, CachedImage>(CACHE_SIZE, 0.75F, true) {
      protected boolean removeEldestEntry(Map.Entry<String, CachedImage> eldest) {
        if (size() <= CACHE_SIZE)
          return false;
        CachedImage image = eldest.getValue();
        return image.mayRemove();
      }
    };


  @ThreadAWT
  @Nullable
  public Image getThumbnail(String imageId, Dimension maxSize) {
    return getThumbnail(imageId, maxSize, null, null, null);
  }

  @ThreadAWT
  @Nullable
  public Image getThumbnail(@NotNull String imageId, Dimension maxSize, ThumbnailSourceFactory sourceFactory,
    ThreadGate factoryGate, ThumbnailReadyNotificator notificator)
  {
    Threads.assertAWTThread();
    if (maxSize.width <= 0 || maxSize.height <= 0)
      return null;
    String cacheId = imageId + "$$" + maxSize.width + "x" + maxSize.height;
    CachedImage cachedImage = myCache.get(cacheId);
    if (cachedImage == null) {
      if (sourceFactory != null && factoryGate != null) {
        cachedImage = new CachedImage(imageId, maxSize, notificator);
        myCache.put(cacheId, cachedImage);
        if (myCache.size() >= CACHE_SIZE) {
          Iterator<Map.Entry<String, CachedImage>> iterator = myCache.entrySet().iterator();
          iterator.next();
          iterator.remove();
        }
        chainCreateSourceImage(cachedImage, sourceFactory, factoryGate);
      }
      return null;
    } else {
      if (cachedImage.isDone()) {
        return cachedImage.getImage();
      } else {
        cachedImage.notifyWhenDone(notificator);
        return null;
      }
    }
  }

  private void chainCreateSourceImage(final CachedImage cachedImage, final ThumbnailSourceFactory sourceImageFactory,
    ThreadGate factoryGate)
  {
    factoryGate.execute(new Runnable() {
      public void run() {
        Image image = sourceImageFactory.createSourceImage(cachedImage.getImageId(), cachedImage.getMaxSize());
        if (image == null) {
          ThreadGate.AWT.execute(new Runnable() {
            public void run() {
              cachedImage.setDone(null);
            }
          });
        } else {
          chainWaitImage(cachedImage, image);
        }
      }
    });
  }

  private void chainWaitImage(final CachedImage cachedImage, final Image image) {
    ThreadGate.LONG.execute(() -> {
      try {
        BufferedImage bufferedImage = Util.castNullable(BufferedImage.class, image);
        Image loaded;
        if (bufferedImage != null) loaded = bufferedImage;
        else {
          ImageLoader loader = ImageLoader.load(image);
          loaded = loader.waitForDone(1000);
        }
        if (loaded == null) LogHelper.warning("Timeout while loading image", cachedImage);
        else
          ThreadGate.AWT.execute(() -> chainCreateThumbnail(cachedImage, loaded));
      } catch (InterruptedException e) {
        cachedImage.setDone(null);
        throw new RuntimeInterruptedException(e);
      }
    });
  }

  @ThreadAWT
  private void chainCreateThumbnail(final CachedImage cachedImage, Image sourceImage) {
    boolean willBeDone = false;
    Graphics2D g = null;
    try {
      int sourceHeight = sourceImage.getHeight(null);
      int sourceWidth = sourceImage.getWidth(null);
      if (sourceHeight <= 0 || sourceWidth <= 0) {
        Log.warn("TI: img " + sourceHeight + " " + sourceWidth + " " + cachedImage);
        return;
      }
      Dimension maxSize = cachedImage.getMaxSize();
      int thumbHeight = maxSize.height;
      int thumbWidth = maxSize.width;

      if (sourceHeight <= thumbHeight && sourceWidth <= thumbWidth) {
        cachedImage.setDone(sourceImage);
        return;
      }

      float thumbRatio = ((float) thumbWidth) / thumbHeight;
      float sourceRatio = ((float) sourceWidth) / sourceHeight;
      if (thumbRatio < sourceRatio) {
        thumbHeight = Math.max((int) (thumbWidth / sourceRatio), 1);
        assert thumbHeight <= maxSize.height;
      } else {
        thumbWidth = Math.max((int) (thumbHeight * sourceRatio), 1);
        assert thumbWidth <= maxSize.width;
      }

      Image scaled = sourceImage.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);
//      final BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
//      g = thumbImage.createGraphics();
//      final int[] flags = {0};
//      final Graphics2D finalG = g;
//      boolean done = g.drawImage(scaled, 0, 0, thumbWidth, thumbHeight, new ImageObserver() {
//        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
//          int f = flags[0] | infoflags;
//          flags[0] = f;
//          if ((f & (ERROR | ABORT)) != 0) {
//            setDone(cachedImage, null, finalG);
//          } else {
//            int needed = ALLBITS | HEIGHT | WIDTH;
//            if ((f & needed) == needed) {
//              setDone(cachedImage, img, finalG);
//            }
//          }
////          return !cachedImage.isDone();
//          return true;
//        }
//      });
      Image thumbImage = scaled;
      boolean done = true;

      if (done) {
        setDone(cachedImage, thumbImage, g);
      } else {
        willBeDone = true;
      }
    } finally {
      if (!cachedImage.isDone() && !willBeDone) {
        setDone(cachedImage, null, g);
      }
    }
  }

  private static void setDone(CachedImage cachedImage, Image img, Graphics2D g) {
    if (!cachedImage.isDone()) {
      cachedImage.setDone(img);
      if (g != null) {
        try {
          g.dispose();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }


  private static class CachedImage {
    private static final long MINIMUM_CACHED_DURATION = 30000;

    private final String myImageId;
    private final Dimension myMaxSize;

    private boolean myDone = false;
    private Image myImage = null;
    private long myDoneTime = 0;

    private ThumbnailReadyNotificator mySingleNotificator;
    private java.util.List<ThumbnailReadyNotificator> myNotificators;

    public CachedImage(String imageId, Dimension maxSize, ThumbnailReadyNotificator notificator) {
      myImageId = imageId;
      myMaxSize = new Dimension(maxSize);
      mySingleNotificator = notificator;
    }

    public boolean mayRemove() {
      return myDone && (System.currentTimeMillis() - myDoneTime > MINIMUM_CACHED_DURATION);
    }

    @ThreadAWT
    public void setDone(Image image) {
      assert !myDone;
      myDone = true;
      myDoneTime = System.currentTimeMillis();
      myImage = image;

      notify(mySingleNotificator);
      mySingleNotificator = null;
      if (myNotificators != null) {
        for (ThumbnailReadyNotificator notificator : myNotificators) {
          notify(notificator);
        }
        myNotificators.clear();
        myNotificators = null;
      }
    }

    private void notify(ThumbnailReadyNotificator notificator) {
      if (notificator != null) {
        try {
          notificator.onThumbnailReady(myImageId, myImage);
        } catch (Exception e) {
          Log.error(e);
        } catch (AssertionError e) {
          Log.error(e);
        }
      }
    }

    @ThreadAWT
    public boolean isDone() {
      return myDone;
    }

    @ThreadAWT
    @Nullable
    public Image getImage() {
      return myImage;
    }

    @ThreadAWT
    public void notifyWhenDone(ThumbnailReadyNotificator notificator) {
      assert !myDone;
      if (notificator == null)
        return;
      if (myNotificators == null)
        myNotificators = Collections15.arrayList();
      myNotificators.add(notificator);
    }

    public String getImageId() {
      return myImageId;
    }


    public String toString() {
      return myImageId;
    }

    public Dimension getMaxSize() {
      return myMaxSize;
    }
  }
}