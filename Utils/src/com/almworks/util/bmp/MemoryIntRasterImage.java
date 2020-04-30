package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.IndexColorModel;

public class MemoryIntRasterImage extends RasterImageSupport implements ChanneledIntRasterImage {
  protected int[] myImageData;

  public MemoryIntRasterImage(int width, int height, ColorModel cm) {
    super(width, height, cm);
    initStorage();
  }

  protected void initStorage() {
    myImageData = new int[getWidth() * getHeight()];
  }

  public void setRectangle(int x, int y, int width, int height, int[] pixels,
    int offset, int scansize) throws BMPException {
    setModified();
    if (myForceRGB) {
      toRGB(pixels, width, height, offset, scansize);
    }
    sendRectangle(x, y, width, height, pixels, offset, scansize);
    storeRectangle(x, y, width, height, pixels, offset, scansize);
  }

  public void storeRectangle(int x, int y, int width, int height, int[] pixels, int offset, int scansize)
    throws BMPException {
    try {
      for (int row = 0; row < height; row++) {
        System.arraycopy(pixels, offset + (row * scansize),
          myImageData, x + (y + row) * getWidth(), width);
      }
    } catch (RuntimeException e) {
      throw new BMPException(e);
    }
  }

  public void setChannelRectangle(int channel, int x, int y, int width, int height, byte[] pixels,
    int offset, int scansize) throws BMPException {
    setModified();
    storeChannelRectangle(channel, x, y, width, height, pixels, offset, scansize);
  }

  public void storeChannelRectangle(int channel, int x, int y, int width, int height, byte[] pixels,
    int offset, int scansize) throws BMPException {
    try {
      for (int row = 0; row < height; row++) {
        for (int column = 0; column < width; column++) {
          myImageData[x + column + (y + row) * getWidth()] |=
            (((int) pixels[offset + column + row * scansize]) & 0xff) << channel;
        }
      }
    } catch (RuntimeException e) {
      throw new BMPException(e);
    }
  }

  protected void sendRectangle(int x, int y, int width, int height, int[] pixels,
    int offset, int scansize) {
    if (!hasDirectConsumer())
      return;
    ImageConsumer consumer = getDirectConsumer();
    consumer.setPixels(x, y, width, height, getColorModel(), pixels, offset, getWidth());
  }

  protected void sendToConsumerFully(ImageConsumer consumer) throws BMPException {
    consumer.setPixels(0, 0, getWidth(), getHeight(), getColorModel(), myImageData, 0, getWidth());
  }

  protected void toRGB(int[] pixels, int width, int height, int offset, int scansize) {
    int pad = width - scansize;
    for (int row = 0; row < height; row++) {
      for (int c = 0; c < width; c++) {
        pixels[offset] = mySourceColorModel.getRGB(pixels[offset]);
        offset++;
      }
      offset += pad;
    }
  }

  protected ColorModel getAppropriateColorModel(ColorModel cm) {
    if (cm instanceof IndexColorModel) {
      return cm;
    }
    if (cm instanceof DirectColorModel) {
      DirectColorModel dcm = (DirectColorModel) cm;
      if ((dcm.getRedMask() == 0xFF0000) &&
        (dcm.getGreenMask() == 0x00FF00) &&
        (dcm.getBlueMask() == 0x0000FF)) {
        return cm;
      } else if (dcm.getAlphaMask() == 0) {
        return new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
      }
    }
    return ColorModel.getRGBdefault();
  }
}

