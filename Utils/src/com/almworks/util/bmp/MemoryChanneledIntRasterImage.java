package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;

public class MemoryChanneledIntRasterImage extends MemoryIntRasterImage {
  public MemoryChanneledIntRasterImage(int w, int h, ColorModel cm) {
    super(w, h, cm);
  }

  public void addDirectConsumer(ImageConsumer consumer) {
    addWaitingConsumer(consumer);
  }
}

