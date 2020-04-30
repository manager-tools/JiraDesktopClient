package com.almworks.util.bmp;

import org.almworks.util.Collections15;

import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.List;

public class ImageProducerProxy implements ImageProducer {
  private final List<ImageConsumer> myConsumers = Collections15.arrayList();
  private ImageProducer myProducer;

  public synchronized void setProducer(ImageProducer producer) {
    myProducer = producer;
    for (int i = 0; i < myConsumers.size(); i++) {
      ImageConsumer consumer = myConsumers.get(i);
      myProducer.addConsumer(consumer);
    }
    myConsumers.clear();
  }

  public synchronized void addConsumer(ImageConsumer consumer) {
    if (myProducer == null) {
      myConsumers.add(consumer);
    } else {
      myProducer.addConsumer(consumer);
    }
  }

  public synchronized boolean isConsumer(ImageConsumer consumer) {
    if (myProducer == null) {
      return myConsumers.contains(consumer);
    } else {
      return myProducer.isConsumer(consumer);
    }
  }

  public synchronized void removeConsumer(ImageConsumer consumer) {
    if (myProducer == null) {
      myConsumers.remove(consumer);
    } else {
      myProducer.removeConsumer(consumer);
    }
  }

  public void startProduction(ImageConsumer consumer) {
    addConsumer(consumer);
  }

  public void requestTopDownLeftRightResend(ImageConsumer consumer) {
    removeConsumer(consumer);
    addConsumer(consumer);
  }
}


