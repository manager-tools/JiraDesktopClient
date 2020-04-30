package com.almworks.util.bmp;

import java.io.IOException;

public class BMPFileHeader {
  final static short FILETYPE_BM = 0x4d42;

  final static short VERSION_2X = 12;	// also OS2 v1.x
  final static short VERSION_3X = 40;
  final static short VERSION_4X = 108;

  private final LEDataInputStream myInput;

  // File Info (14 bytes):
  short fileType = 0x4d42;// always "BM"
  int fileSize;			// size of file in bytes
  short reserved1 = 0;	// always 0
  short reserved2 = 0;	// always 0
  int bitmapOffset;		// starting byte position of image data

  // Image Info for v2.x (12 bytes):
  // width/height red as shorts for v2.x
  int size;				// size of this header in bytes
  int width;				// image width in pixels
  int height;				// image height in pixels (if < 0, "top-down")
  int planes;				// unsigned short - no. of color planes: always 1
  int bitsPerPixel;		// unsigned short - number of bits per pixel: 1, 4, 8, or 24 (no color map)

  // Extra Image Info for v3.x (40 bytes):
  int compression;		// compression methods used: 0 (none), 1 (8-bit RLE), or 2 (4-bit RLE)
  int sizeOfBitmap;		// size of bitmap in bytes (may be 0: if so, calculate)
  int horzResolution;		// horizontal resolution, pixels/meter (may be 0)
  int vertResolution;		// vertical resolution, pixels/meter (may be 0)
  int colorsUsed;			// no. of colors in palette (if 0, calculate)
  int colorsImportant;	// no. of important colors (appear first in palette) (0 means all are important)

  // Extra Image Info for v3.x NT extensions
  int redMask;
  int greenMask;
  int blueMask;

  // Extra Image Info for v4.x extensions (108 bytes)
  int alphaMask;
  int csType;
  int redX;
  int redY;
  int redZ;
  int greenX;
  int greenY;
  int greenZ;
  int blueX;
  int blueY;
  int blueZ;
  int gammaRed;
  int gammaGreen;
  int gammaBlue;

  // Calculated values:
  boolean topDown;
  int actualSizeOfBitmap;
  int scanLineSize;
  int actualColorsUsed;
  int noOfPixels;
  int bmpVersion;			// possible values 2, 3, 4

  void readVersion2x() throws IOException {
    width = myInput.readShort();
    height = myInput.readShort();
    planes = myInput.readUnsignedShort();
    bitsPerPixel = myInput.readUnsignedShort();

    compression = 0;	// default it
    bmpVersion = 2;
    topDown = (height < 0);
  }

  /**
   * There is an undocumented 16 bit version of v3.x bitmap
   * that does not have compression set to 3.
   */
  void readVersion3x() throws IOException {
    width = myInput.readInt();
    height = myInput.readInt();
    planes = myInput.readUnsignedShort();
    bitsPerPixel = myInput.readUnsignedShort();

    compression = myInput.readInt();
    sizeOfBitmap = myInput.readInt();
    horzResolution = myInput.readInt();
    vertResolution = myInput.readInt();
    colorsUsed = myInput.readInt();
    colorsImportant = myInput.readInt();

    if (compression == 3)	// pick up v3.x NT extension.
    {
      redMask = myInput.readInt();
      greenMask = myInput.readInt();
      blueMask = myInput.readInt();
    } else {
      // undocumented 16 bits/pixel non compression mode 3 file.
      if (bitsPerPixel == 16)		// 5 bits of each
      {
        redMask = 0x1F << 10;
        greenMask = 0x1F << 5;
        blueMask = 0x1F;
        alphaMask = 0;
      }
    }

    bmpVersion = 3;
    topDown = (height < 0);
  }

  void readVersion4x() throws IOException {
    width = myInput.readInt();
    height = myInput.readInt();
    planes = myInput.readUnsignedShort();
    bitsPerPixel = myInput.readUnsignedShort();

    compression = myInput.readInt();
    sizeOfBitmap = myInput.readInt();
    horzResolution = myInput.readInt();
    vertResolution = myInput.readInt();
    colorsUsed = myInput.readInt();
    colorsImportant = myInput.readInt();

    redMask = myInput.readInt();
    greenMask = myInput.readInt();
    blueMask = myInput.readInt();
    alphaMask = myInput.readInt();
    csType = myInput.readInt();
    redX = myInput.readInt();
    redY = myInput.readInt();
    redZ = myInput.readInt();
    greenX = myInput.readInt();
    greenY = myInput.readInt();
    greenZ = myInput.readInt();
    blueX = myInput.readInt();
    blueY = myInput.readInt();
    blueZ = myInput.readInt();
    gammaRed = myInput.readInt();
    gammaGreen = myInput.readInt();
    gammaBlue = myInput.readInt();

    bmpVersion = 4;
    topDown = (height < 0);
  }

  public BMPFileHeader(LEDataInputStream in) throws IOException, BMPException {
    myInput = in;
    fileType = myInput.readShort();
    if (fileType != FILETYPE_BM)
      throw new BMPException("Not a bmp file");
    fileSize = myInput.readInt();
    reserved1 = myInput.readShort();
    reserved2 = myInput.readShort();
    bitmapOffset = myInput.readInt();
    size = myInput.readInt();	// header size
    if (size == VERSION_2X)
      readVersion2x();
    else if (size == VERSION_3X)
      readVersion3x();
    else if (size == VERSION_4X)
      readVersion4x();
    else
      throw new BMPException("Unsupported version " + size);
    if (topDown)
      throw new BMPException("Unsupported topdown bmp");
    noOfPixels = width * height;
    scanLineSize = ((width * bitsPerPixel + 31) / 32) * 4;
    if (sizeOfBitmap != 0)
      actualSizeOfBitmap = sizeOfBitmap;
    else
      actualSizeOfBitmap = scanLineSize * height;

    if (colorsUsed != 0) {
      actualColorsUsed = colorsUsed;
    } else {
      if (bitsPerPixel < 16) {
        actualColorsUsed = 1 << bitsPerPixel;
      } else {
        actualColorsUsed = 0;
      }
    }
  }
}

