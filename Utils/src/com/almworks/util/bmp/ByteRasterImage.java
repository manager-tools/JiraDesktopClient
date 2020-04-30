package com.almworks.util.bmp;

public interface ByteRasterImage extends BMPImage {
  public void setRectangle(int x, int y, int width, int height, byte[] pixels, int offset, int scansize)
    throws BMPException;
}
