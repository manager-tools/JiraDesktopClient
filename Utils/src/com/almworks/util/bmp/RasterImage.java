package com.almworks.util.bmp;

import java.awt.image.ColorModel;

public class RasterImage {
  private static final int MODE_INT = 0;
  private static final int MODE_BYTE = 1;
  private static final int MODE_BIT = 2;
  private static final int MODE_CHANNELED = 3;

  private int myMode;

  private ColorModel myColorModel;
  private int myWidth;
  private int myHeight;

  private IntRasterImage intImage;
  private ChanneledIntRasterImage channelImage;
  private ByteRasterImage byteImage;

  private BMPImage mutableImage;
  private BMPDecoder decoder;

  public RasterImage(BMPDecoder decoder) {
    setDecoder(decoder);
  }

  public void setDecoder(BMPDecoder decoder) {
    this.decoder = decoder;
  }

  public void setSize(int width, int height) {
    this.myWidth = width;
    this.myHeight = height;
  }

  public void setColorModel(ColorModel cm) {
    this.myColorModel = cm;
  }

  public void setPixels() throws BMPException {
    createBackEnd();
  }

  private void createBackEnd() throws BMPException {
    int pixelSize = myColorModel.getPixelSize();
    // bit-per-pixel
    if (pixelSize == 1) {
      mutableImage = byteImage = new MemoryBitRasterImage(myWidth, myHeight, myColorModel);
      myMode = MODE_BIT;
    }
    // pixel fits in a byte?
    else if (pixelSize <= 8) {
      mutableImage = byteImage = new MemoryByteRasterImage(myWidth, myHeight, myColorModel);
      myMode = MODE_BYTE;
    }
    // default to int
    else {
      if (decoder != null && decoder.usesChanneledData()) {
        mutableImage = intImage = channelImage = new MemoryChanneledIntRasterImage(myWidth, myHeight, myColorModel);
        myMode = MODE_CHANNELED;
      } else {
        mutableImage = intImage = new MemoryIntRasterImage(myWidth, myHeight, myColorModel);
        myMode = MODE_INT;
      }
    }
    if (decoder != null)
      decoder.onImageCreated(mutableImage);
  }

  public void addFullCoverage() throws BMPException {
    if (mutableImage != null)
      mutableImage.setFinished();
  }

  /*
   * setChannel method mappings.
   *
   * setChannel methods are used for storing pixel data in a variety of ways.
   */

  /**
   * Set an 8-bit rectangular channel.
   */
  public synchronized void setChannel(int channel, int x, int y, int w, int h,
    byte[] pixels, int off, int scansize)
    throws BMPException {
    switch (myMode) {
    case MODE_CHANNELED:
      channelImage.setChannelRectangle(channel, x, y, w, h, pixels, off, scansize);
      break;
    case MODE_INT:
      throw new BMPException();

    case MODE_BYTE:
      byteImage.setRectangle(x, y, w, h, pixels, off, scansize);
      break;

    case MODE_BIT:
      byteImage.setRectangle(x, y, w, h, pixels, off, scansize);
    }
  }

  /**
   * Set a 32-bit rectangular channel.
   */
  public void setChannel(int x, int y, int w, int h,
    int[] pixels, int off, int scansize)
    throws BMPException {
    switch (myMode) {
    case MODE_INT:
    case MODE_CHANNELED:
      intImage.setRectangle(x, y, w, h, pixels, off, scansize);
      break;
    default:
      throw new BMPException();
    }
  }

  /**
   * Set an 8-bit channel row.
   */
  public void setChannel(int channel, int row, byte[] pixels, int off, int len)
    throws BMPException {
    setChannel(channel, 0, row, myWidth, 1, pixels, off, len);
  }

  /**
   * Set an 8-bit channel row.
   */
  public void setChannel(int channel, int row, byte[] pixels)
    throws BMPException {
    setChannel(channel, row, pixels, 0, myWidth);
  }

  /**
   * Set a 32-bit channel row.
   */
  public void setChannel(int row, int[] pixels)
    throws BMPException {
    setChannel(0, row, myWidth, 1, pixels, 0, myWidth);
  }

  /**
   * Initialize the image with a specified long value.
   *
   * @deprecated Obsolete.
   */
  public void setChannel(long val)
    throws BMPException {
  }

}
