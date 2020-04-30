package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;

public abstract class RasterImageSupport implements BMPImage, ImageProducer {
  private static final int DEFAULT_HINTS = ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES |
    ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME;

  protected ColorModel myColorModel;
  protected boolean myFinished = false;
  protected boolean myForceRGB = false;
  protected boolean myModified = false;
  protected boolean myProductionStarted = false;

  protected ColorModel mySourceColorModel = null;

  private MulticastImageConsumer myDirectConsumer = new MulticastImageConsumer();
  private MulticastImageConsumer myWaitingConsumer = new MulticastImageConsumer();

  private int myHeight;
  private int myWidth;

  private int hints = DEFAULT_HINTS;

  protected RasterImageSupport(int w, int h, ColorModel cm) {
    myWidth = w;
    myHeight = h;
    setColorModel(cm);
  }

  public synchronized void setFinished() throws BMPException {
    if (myFinished)
      return;
    myFinished = true;
    myDirectConsumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
    myDirectConsumer.removeAll();
    if (!myWaitingConsumer.isEmpty()) {
      try {
        sendToConsumerFully(myWaitingConsumer);
      } catch (BMPException e) {
        setError();
        throw e;
      }
    }
    myWaitingConsumer.getConsumers();
    myWaitingConsumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
    myWaitingConsumer.removeAll();
    notifyAll();
  }

  public ImageProducer getImageProducer() {
    return this;
  }

  public synchronized void addConsumer(ImageConsumer consumer) {
    initConsumer(consumer);
    if (!myProductionStarted) {
      addDirectConsumer(consumer);
      myProductionStarted = true;
    } else {
      addWaitingConsumer(consumer);
    }
  }

  public void removeConsumer(ImageConsumer consumer) {
    myDirectConsumer.removeConsumer(consumer);
    myWaitingConsumer.removeConsumer(consumer);
  }

  public void requestTopDownLeftRightResend(ImageConsumer consumer) {
    addWaitingConsumer(consumer);
  }

  public void startProduction(ImageConsumer consumer) {
    removeConsumer(consumer);
    addConsumer(consumer);
  }

  public boolean isConsumer(ImageConsumer consumer) {
    return myDirectConsumer.contains(consumer) || myWaitingConsumer.contains(consumer);
  }

  public ColorModel getColorModel() {
    return myColorModel;
  }

  public int getHeight() {
    return myHeight;
  }

  public int getWidth() {
    return myWidth;
  }

  public void setColorModel(ColorModel cm) {
    if (!myForceRGB) {
      myColorModel = getAppropriateColorModel(cm);
      myForceRGB |= (cm != myColorModel);
    }
    mySourceColorModel = cm;
  }

  public synchronized void setError() {
    myDirectConsumer.imageComplete(ImageConsumer.IMAGEERROR);
    myWaitingConsumer.imageComplete(ImageConsumer.IMAGEERROR);
    notifyAll();
  }

  protected void addDirectConsumer(ImageConsumer consumer) {
    consumer.setDimensions(getWidth(), getHeight());
    myDirectConsumer.addConsumer(consumer);
    consumer.setHints(hints);
  }

  protected synchronized void addWaitingConsumer(ImageConsumer consumer) {
    consumer.setDimensions(getWidth(), getHeight());
    consumer.setHints(DEFAULT_HINTS);
    if (myFinished) {
      try {
        sendToConsumerFully(consumer);
        consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
      } catch (BMPException e) {
        consumer.imageComplete(ImageConsumer.IMAGEERROR);
      }
    } else {
      myWaitingConsumer.addConsumer(consumer);
    }
  }

  protected ColorModel getAppropriateColorModel(ColorModel cm) {
    return cm;
  }

  protected MulticastImageConsumer getDirectConsumer() {
    return myDirectConsumer;
  }

  protected boolean hasDirectConsumer() {
    return !myDirectConsumer.isEmpty();
  }

  protected void initConsumer(ImageConsumer consumer) {
    consumer.setColorModel(getColorModel());
  }

  protected abstract void sendToConsumerFully(ImageConsumer consumer) throws BMPException;

  protected void setModified() {
    if (!myModified) {
      myModified = true;
      myDirectConsumer.setHints(hints);
      myProductionStarted = true;
    }
  }
}

