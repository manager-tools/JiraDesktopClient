package com.almworks.util.bmp;



import org.almworks.util.Collections15;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.util.Hashtable;
import java.util.List;

public class MulticastImageConsumer implements ImageConsumer {
  private List<ImageConsumer> myConsumers = Collections15.arrayList();
  private ImageConsumer[] myConsumerCache;
  private boolean myCacheValid = false;

  public void setDimensions(int w, int h) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setDimensions(w, h);
  }

  public void setColorModel(ColorModel cm) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setColorModel(cm);
  }

  public void setHints(int hints) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setHints(hints);
  }

  public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setPixels(x, y, w, h, model, pixels, off, scansize);
  }

  public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setPixels(x, y, w, h, model, pixels, off, scansize);
  }

  public void imageComplete(int status) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].imageComplete(status);
  }

  public void setProperties(Hashtable properties) {
    ImageConsumer[] consumers = getConsumers();
    for (int i = 0; i < consumers.length; i++)
      consumers[i].setProperties(properties);
  }

  public synchronized ImageConsumer[] getConsumers() {
    if (!myCacheValid) {
      myConsumerCache = myConsumers.toArray(new ImageConsumer[myConsumers.size()]);
      myCacheValid = true;
    }
    return myConsumerCache;
  }

  public synchronized void addConsumer(ImageConsumer consumer) {
    myCacheValid = false;
    myConsumers.add(consumer);
  }

  public void removeConsumer(ImageConsumer consumer) {
    myCacheValid = false;
    myConsumers.remove(consumer);
    myConsumerCache = null;
  }

  public void removeAll() {
    myCacheValid = false;
    myConsumers.clear();
    myConsumerCache = null;
  }

  public boolean isEmpty() {
    return myConsumers.size() == 0;
  }

  public boolean contains(ImageConsumer consumer) {
    return myConsumers.contains(consumer);
  }
}
