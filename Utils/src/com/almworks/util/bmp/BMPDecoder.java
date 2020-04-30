package com.almworks.util.bmp;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BMPDecoder {
  public final static int ERROR = 0x0001;
  public final static int INFOAVAIL = 0x0002;
  public final static int IMAGEAVAIL = 0x0004;

  private byte[] myByteScanLine;
  private int[] myIntScanLine;
  private byte[] myRawScanLine;

  private BMPImageProxy myCurrentProxy;

  private boolean myFinishedDecoding;
  private BMPColorMap myBmpColorMap;
  private BMPFileHeader myBmpHeader;
  private ColorModel myModel;
  private LEDataInputStream myLeInput;

  private RasterImage myImage;
  private int myState;
  /**
   * COMMENT!
   */
  static byte[] expansionTable = new byte[256 * 8];

  static {
    int index = 0;
    for (int i = 0; i < 256; i++) {
      expansionTable[index++] = (i & 128) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 64) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 32) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 16) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 8) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 4) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 2) == 0 ? (byte) 0 : (byte) 1;
      expansionTable[index++] = (i & 1) == 0 ? (byte) 0 : (byte) 1;
    }
  }

  public BMPImage decode() throws BMPException {
    myCurrentProxy = new BMPImageProxy();
    if (myFinishedDecoding)
      throw new BMPException("already decoded");
    try {
      myBmpHeader = new BMPFileHeader(myLeInput);
      myBmpColorMap = new BMPColorMap(myLeInput, myBmpHeader);
      initBMPImage();
      myState |= INFOAVAIL;
      loadImage(myLeInput);
      myImage.addFullCoverage();
      myState |= IMAGEAVAIL;
    } catch (IOException e) {
      myState |= ERROR;
      throw new BMPException(e);
    } catch (BMPException e) {
      myState |= ERROR;
      throw e;
    }
    if ((myState & ERROR) != 0 || (!myCurrentProxy.isImageSet()))
      throw new BMPException();
    myFinishedDecoding = true;
    return myCurrentProxy;
  }

  private void initBMPImage() throws BMPException {
    myImage.setSize(myBmpHeader.width, myBmpHeader.height);
    if (myBmpHeader.bitsPerPixel == 32) {
      myModel = ColorModel.getRGBdefault();
    } else if (myBmpHeader.bitsPerPixel == 24) {
      myModel = new DirectColorModel(24, 0xFF0000, 0xFF00, 0xFF);
    } else if (myBmpHeader.bitsPerPixel == 16) {
      myModel = new DirectColorModel(16, myBmpHeader.redMask, myBmpHeader.greenMask,
        myBmpHeader.blueMask, myBmpHeader.alphaMask);
    } else {
      if (!(myBmpColorMap.N > 0))
        throw new BMPException("no paletter");
      myModel = new IndexColorModel(8, myBmpColorMap.N, myBmpColorMap.r, myBmpColorMap.g, myBmpColorMap.b);
    }
    myImage.setColorModel(myModel);
    myImage.setPixels();
    if (myBmpHeader.bitsPerPixel == 32) {
      myIntScanLine = new int[myBmpHeader.scanLineSize / 4];
    }
    if (myBmpHeader.bitsPerPixel == 24) {

      myIntScanLine = new int[myBmpHeader.scanLineSize / 3];
    }
    if (myBmpHeader.bitsPerPixel == 16) {
      myIntScanLine = new int[myBmpHeader.scanLineSize / 2];
    } else {
      myByteScanLine = new byte[myBmpHeader.width];
    }
    myRawScanLine = new byte[myBmpHeader.scanLineSize];
  }

  private void loadImage(LEDataInputStream leInput) throws BMPException, IOException {
    switch (myBmpHeader.compression) {
    case 0:
      break;

    case 1:
      myImage.setChannel(0);
      unpackRLE8(leInput);
      return;

    case 2:
      myImage.setChannel(0);
      unpackRLE4(leInput);
      return;

    case 3:
      break;

    default:
      throw new BMPException("Compression " + myBmpHeader.compression);
    }


    for (int i = myBmpHeader.height - 1; i >= 0; i--) {
      leInput.readFully(myRawScanLine, 0, myBmpHeader.scanLineSize);

      if (myBmpHeader.bitsPerPixel == 32) {
        pack32ToInt(myRawScanLine, 0, myIntScanLine, 0, myBmpHeader.width);
        myImage.setChannel(i, myIntScanLine);
      } else if (myBmpHeader.bitsPerPixel == 24) {

        pack24ToInt(myRawScanLine, 0, myIntScanLine, 0, myBmpHeader.width);
        myImage.setChannel(i, myIntScanLine);
      } else if (myBmpHeader.bitsPerPixel == 16) {
        pack16ToInt(myRawScanLine, 0, myIntScanLine, 0, myBmpHeader.width);
        myImage.setChannel(i, myIntScanLine);
      } else {
        if (myBmpHeader.bitsPerPixel < 8) {
          expandPixels(myBmpHeader.bitsPerPixel, myRawScanLine, myByteScanLine, myBmpHeader.width);
          myImage.setChannel(0, i, myByteScanLine);
        } else {
          myImage.setChannel(0, i, myRawScanLine, 0, myBmpHeader.width);
        }
      }
    }
  }

  private void pack32ToInt(byte[] rawData, int rawOffset, int[] intData, int intOffset, int w) {
    int j = intOffset;
    int k = rawOffset;
    int mask = 0xff;
    for (int i = 0; i < w; i++) {
      int b0 = (((int) (rawData[k++])) & mask);
      int b1 = (((int) (rawData[k++])) & mask) << 8;
      int b2 = (((int) (rawData[k++])) & mask) << 16;
      int b3 = (((int) (rawData[k++])) & mask) << 24;
      intData[j] = 0xff000000 | b0 | b1 | b2;
      j++;
    }
  }

  private void pack24ToInt(byte[] rawData, int rawOffset, int[] intData, int intOffset, int w) {
    int j = intOffset;
    int k = rawOffset;
    int mask = 0xff;
    for (int i = 0; i < w; i++) {
      int b0 = (((int) rawData[k++]) & mask);
      int b1 = (((int) rawData[k++]) & mask) << 8;
      int b2 = (((int) rawData[k++]) & mask) << 16;
      intData[j] = 0xff000000 | b0 | b1 | b2;
      j++;
    }
  }

  private void pack16ToInt(byte[] rawData, int rawOffset, int[] intData, int intOffset, int w) {
    int j = intOffset;
    int k = rawOffset;
    int mask = 0xff;

    for (int i = 0; i < w; i++) {
      int b0 = (rawData[k++] & mask);
      int b1 = (rawData[k++] & mask) << 8;
      intData[j] = b0 | b1;
      j++;
    }
  }

  public void initDecoder(InputStream in, RasterImage image) throws BMPException {
    myBmpHeader = null;
    myBmpColorMap = null;
    myLeInput = new LEDataInputStream(new BufferedInputStream(in));
    myImage = image;
    myState = 0;
  }

  public void initDecoding(InputStream input) throws BMPException {
    myImage = new RasterImage(this);
    myImage.setDecoder(this);
    initDecoder(input, myImage);
  }

  public void setFinished() {
  }

  public boolean usesChanneledData() {
    return true;
  }

  protected void onImageCreated(BMPImage image) {
    myCurrentProxy.setImage(image);
  }


  void unpackRLE4(InputStream in) throws BMPException, IOException {
    int i;
    int b;
    int repB;
    int flagB;
    boolean oddSkip;
    byte[] buf;
    int row = myBmpHeader.height - 1;
    int col = 0;
    buf = new byte[myBmpHeader.width];
    int o = 0;

    while (true) {
      b = in.read();
      if (b < 0)
        throw new EOFException();
      else if (b == 0) {

        flagB = in.read();
        if (flagB < 0)
          throw new EOFException();
        switch (flagB) {
        case 0:
          if (o > col)
            myImage.setChannel(0, col, row, o - col, 1,
              buf, col, buf.length);
          o = 0;
          col = 0;
          --row;
          break;

        case 1:
          return;

        case 2:
          if (o > col)
            myImage.setChannel(0, col, row, o - col, 1,
              buf, col, buf.length);

          int xDelta = in.read();
          int yDelta = in.read();

          col += xDelta;
          o = col;
          row += yDelta;
          break;

        default:
          oddSkip = false;
          if (((flagB & 0x3) == 0x1) || ((flagB & 0x3) == 0x2))
            oddSkip = true;
          int nybI = 0;
          byte[] nybles = new byte[2];
          while (--flagB >= 0) {
            if (nybI == 0) {
              int b2 = in.read();
              if (b2 < 0)
                throw new EOFException();
              nybles[0] = (byte) ((b2 & 0xF0) >> 4);
              nybles[1] = (byte) (b2 & 0xF);
            }

            if (o == buf.length) {
              if (o > col)
                myImage.setChannel(0, col, row, o - col, 1,
                  buf, col, buf.length);
              o = 0;
              col = 0;
              --row;
            }
            buf[o++] = (byte) nybles[nybI];
            nybI ^= 0x1;
          }
          if (oddSkip)
            in.read();
          break;
        }
      } else {
        repB = in.read();
        if (repB < 0)
          throw new EOFException();
        byte[] nybles = new byte[2];
        nybles[0] = (byte) ((repB & 0xF0) >> 4);
        nybles[1] = (byte) (repB & 0xF);
        int nybI = 0;
        while (--b >= 0) {
          if (o == buf.length) {
            if (o > col) {
              myImage.setChannel(0, col, row, o - col, 1,
                buf, col, buf.length);
            }
            o = 0;
            col = 0;
            --row;
          }
          buf[o++] = nybles[nybI];
          nybI ^= 0x1;
        }
      }
    }
  }

  void unpackRLE8(InputStream in) throws BMPException, IOException {
    int i;
    int b;
    int repB;
    int flagB;
    boolean oddSkip;
    byte[] buf;

    int row = myBmpHeader.height - 1;
    int col = 0;
    buf = new byte[myBmpHeader.width];
    int o = 0;

    while (true) {
      b = in.read();
      if (b < 0)
        throw new EOFException();
      else if (b == 0) {

        flagB = in.read();
        if (flagB < 0)
          throw new EOFException();
        switch (flagB) {
        case 0:

          if (o > col) {
            myImage.setChannel(0, col, row, o - col, 1,
              buf, col, buf.length);
          }
          o = 0;
          col = 0;
          --row;
          break;

        case 1:
          return;

        case 2:
          myImage.setChannel(0, col, row, o - col, 1,
            buf, col, buf.length);

          int xDelta = in.read();
          int yDelta = in.read();


          col += xDelta;
          o = col;
          row += yDelta;
          break;

        default:
          oddSkip = false;
          if ((flagB & 0x1) != 0)
            oddSkip = true;
          while (--flagB >= 0) {
            int b2 = in.read();
            if (b2 < 0)
              throw new EOFException();
            if (o == buf.length) {
              myImage.setChannel(0, col, row, o - col, 1,
                buf, col, buf.length);
              o = 0;
              col = 0;
              --row;

            }
            buf[o++] = (byte) b2;
          }
          if (oddSkip)
            in.read();
          break;
        }
      } else {

        repB = in.read();
        if (repB < 0)
          throw new EOFException();
        while (--b >= 0) {
          if (o == buf.length) {

            myImage.setChannel(0, col, row, o - col, 1,
              buf, col, buf.length);
            o = 0;
            col = 0;
            --row;
          }
          buf[o++] = (byte) repB;
        }
      }
    }
  }

  public static void expandPixels(int bitSize, byte[] in, byte[] out, int outLen) {
    if (bitSize == 1) {
      expandOneBitPixels(in, out, outLen);
      return;
    }
    int i;
    int o;
    int t;
    byte v;
    int count = 0;
    int maskshift = 1;
    int pixelshift = 0;
    int tpixelshift = 0;
    int pixelshiftdelta = 0;
    int mask = 0;
    int tmask;

    switch (bitSize) {
    case 1:
      mask = 0x80;
      maskshift = 1;
      count = 8;
      pixelshift = 7;
      pixelshiftdelta = 1;
      break;
    case 2:
      mask = 0xC0;
      maskshift = 2;
      count = 4;
      pixelshift = 6;
      pixelshiftdelta = 2;
      break;
    case 4:
      mask = 0xF0;
      maskshift = 4;
      count = 2;
      pixelshift = 4;
      pixelshiftdelta = 4;
      break;
    default:
      throw new RuntimeException("support only expand for 1, 2, 4");
    }


    i = 0;
    for (o = 0; o < out.length;) {
      tmask = mask;
      tpixelshift = pixelshift;
      v = in[i];
      for (t = 0; t < count && o < out.length; ++t, ++o) {

        out[o] = (byte) (((v & tmask) >>> tpixelshift) & 0xFF);
        tmask = (byte) ((tmask & 0xFF) >>> maskshift);
        tpixelshift -= pixelshiftdelta;
      }
      ++i;
    }
  }

  public static void expandOneBitPixels(byte[] input, byte[] output, int count) {

    int remainder = count % 8;
    int max = count / 8;
    for (int i = 0; i < max; i++) {

      int lookup = (input[i] & 0xFF) * 8;
      System.arraycopy(expansionTable, lookup, output, i * 8, 8);

    }
    if (remainder != 0) {
      System.arraycopy(expansionTable, (input[max] & 0xff) * 8, output, max * 8, remainder);
    }

  }
}

