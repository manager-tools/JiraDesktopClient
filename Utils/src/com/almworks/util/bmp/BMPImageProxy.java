package com.almworks.util.bmp;

import java.awt.image.ImageProducer;

public class BMPImageProxy implements BMPImage {
  private BMPImage myImage;
  private final ImageProducerProxy myProducerProxy = new ImageProducerProxy();

  public synchronized void setImage(BMPImage image) {
    myImage = image;
    myProducerProxy.setProducer(image.getImageProducer());

    // notification for waitImageSet()
    notifyAll();
  }

  public boolean isImageSet() {
    return myImage != null;
  }

  public BMPImage getWrappedBMPImage() throws BMPException {
    if (myImage == null)
      throw new BMPException();
    return myImage;
  }

  public ImageProducer getImageProducer() {
    return myProducerProxy;
  }

  public void setFinished() {
  }
}

