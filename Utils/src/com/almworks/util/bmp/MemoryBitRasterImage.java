package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.IndexColorModel;

public class MemoryBitRasterImage extends RasterImageSupport implements ByteRasterImage {
  protected byte[] myImageData;
  protected byte[] myRowUnpackedBuffer;
  protected int myRowByteWidth;

  public MemoryBitRasterImage(int width, int height, ColorModel cm) throws BMPException {
    super(width, height, cm);
    int color0 = cm.getRGB(0);
    int color1 = cm.getRGB(1);
    byte[] alphas = new byte[]{(byte) ((color0 >> 24) & 0xff), (byte) ((color1 >> 24) & 0xff)};
    byte[] reds = new byte[]{(byte) ((color0 >> 16) & 0xff), (byte) ((color1 >> 16) & 0xff)};
    byte[] greens = new byte[]{(byte) ((color0 >> 8) & 0xff), (byte) ((color1 >> 8) & 0xff)};
    byte[] blues = new byte[]{(byte) ((color0 >> 0) & 0xff), (byte) ((color1 >> 0) & 0xff)};
    IndexColorModel icm = new IndexColorModel(8, 2, reds, greens, blues, alphas);
    setColorModel(icm);
    myRowByteWidth = (width + 7) / 8;
    try {
      initStorage();
    } catch (BMPException e) {
      setError();
      throw e;
    }
  }

  protected void initStorage() throws BMPException {
    myImageData = new byte[myRowByteWidth * getHeight()];
    myRowUnpackedBuffer = new byte[myRowByteWidth * 8];
  }

  public void setRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException {
    setModified();
    sendRectangle(x, y, width, height, pixels, offset, scansize);
    storeRectangle(x, y, width, height, pixels, offset, scansize);
  }

  public void sendRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException {
    if (!hasDirectConsumer())
      return;
    ImageConsumer consumer = getDirectConsumer();
    consumer.setPixels(x, y, width, height, getColorModel(), pixels, offset, scansize);
  }

  public void storeRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException {
    for (int row = 0; row < height; row++) {
      packOneBitPixels(pixels, offset + (row * scansize), myImageData, (y + row) * myRowByteWidth, x, width);
    }
  }

  protected void sendToConsumerFully(ImageConsumer consumer) {
    int width = getWidth();
    int height = getHeight();
    for (int row = 0; row < height; row++) {
      expandOneBitPixels(myImageData, myRowUnpackedBuffer, width,
        myRowByteWidth * 8 * row, 0);
      consumer.setPixels(0, row, width, 1, getColorModel(), myRowUnpackedBuffer, 0, 0);
    }
  }

  public static void expandOneBitPixels(byte[] input, byte[] output, int count, int inputOffset, int outputOffset) {
    int leadingBits = inputOffset % 8;
    inputOffset /= 8;
    if (leadingBits != 0) {
      leadingBits = 8 - leadingBits;
      System.arraycopy(BMPDecoder.expansionTable, ((((int) input[inputOffset++]) & 0xff) << 3) + 8 - leadingBits,
        output, outputOffset, leadingBits);
      outputOffset += leadingBits;
    }
    int remainder = count % 8;
    int max = inputOffset + count / 8;
    for (int i = inputOffset; i < max; i++) {
      System.arraycopy(BMPDecoder.expansionTable, (input[i] & 0xFF) << 3, output, outputOffset, 8);
      outputOffset += 8;
    }
    if (remainder != 0) {
      System.arraycopy(BMPDecoder.expansionTable, (input[max - 1] & 0xff) << 3, output, outputOffset, remainder);
    }
  }

  public static void packOneBitPixels(byte[] in, int inByte, byte[] out, int base, int outPixel, int len) {
    int src_index = inByte;
    base += outPixel / 8;
    outPixel %= 8;
    int count = outPixel == 0 ? len / 8 : (len - (8 - outPixel)) / 8;
    int work = 0;
    int start_idx = outPixel == 0 ? 0 : 1;
    int dest_index = base + (outPixel == 0 ? 0 : 1);
    int leading_bits = (len < (8 - outPixel)) ? len : 8 - outPixel;
    if (outPixel != 0) {
      int first_pixel = 7 - outPixel;
      for (int i = 0; i < leading_bits; i++) {
        if (in[src_index++] == 0)
          out[base] &= ~(1 << (first_pixel - i));
        else
          out[base] |= 1 << (first_pixel - i);
      }
    }
    for (int i = start_idx; i < count; i++) {
      work = 0;
      if ((in[src_index++]) != 0) {
        work += 128;
      }
      if ((in[src_index++]) != 0) {
        work += 64;
      }
      if ((in[src_index++]) != 0) {
        work += 32;
      }
      if ((in[src_index++]) != 0) {
        work += 16;
      }
      if ((in[src_index++]) != 0) {
        work += 8;
      }
      if ((in[src_index++]) != 0) {
        work += 4;
      }
      if ((in[src_index++]) != 0) {
        work += 2;
      }
      if ((in[src_index++]) != 0) {
        work += 1;
      }
      out[dest_index + i] = (byte) work;
    }

    int remainder;
    if (outPixel == 0)
      remainder = len % 8;
    else
      remainder = len - (8 - outPixel) % 8;
    remainder %= 8;
    int last_index = base + count;
    if (remainder > 0) {
      for (int i = 0; i < remainder; i++) {
        if (in[src_index++] == 0)
          out[last_index] &= ~(1 << (7 - i));
        else
          out[last_index] |= 1 << (7 - i);
      }
    }
  }
}

