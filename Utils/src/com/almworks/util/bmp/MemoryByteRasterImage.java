package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.IndexColorModel;

public class MemoryByteRasterImage extends RasterImageSupport implements ByteRasterImage {
  protected byte[] myImageData;

  public MemoryByteRasterImage(int width, int height, ColorModel cm) {
    super(width, height, cm);
    initStorage();
  }

  protected void initStorage() {
    myImageData = new byte[getWidth() * getHeight()];
  }

  public void setRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException {
    setModified();
    sendRectangle(x, y, width, height, pixels, offset, scansize);
    storeRectangle(x, y, width, height, pixels, offset, scansize);
  }

  public void storeRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException {
    try {
      for (int row = 0; row < height; row++) {
        System.arraycopy(pixels, offset + (row * scansize), myImageData, x + (y + row) * getWidth(), width);
      }
    } catch (RuntimeException e) {
      throw new BMPException(e);
    }
  }


  protected void sendRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize) {
    if (!hasDirectConsumer())
      return;
    ImageConsumer consumer = getDirectConsumer();
    consumer.setPixels(x, y, width, height, getColorModel(), pixels, offset, getWidth());
  }

  protected void sendToConsumerFully(ImageConsumer consumer) throws BMPException {
    consumer.setPixels(0, 0, getWidth(), getHeight(), getColorModel(), myImageData, 0, getWidth());
  }

  protected ColorModel getAppropriateColorModel(ColorModel cm) {
    if (cm instanceof IndexColorModel)
      return cm;
    byte[] reds = new byte[256];
    byte[] greens = new byte[256];
    byte[] blues = new byte[256];
    byte[] alphas = new byte[256];
    for (int i = 0; i < 256; i++) {
      int rgb = cm.getRGB(i);
      alphas[i] = (byte) (rgb >>> 24);
      reds[i] = (byte) (rgb >>> 16);
      greens[i] = (byte) (rgb >>> 8);
      blues[i] = (byte) rgb;
    }
    cm = new IndexColorModel(8, 256, reds, greens, blues);
    return cm;
  }

}

