package com.almworks.util.bmp;

public interface IntRasterImage extends BMPImage {
  public void setRectangle(int x, int y, int width, int height, int[] pixels, int offset, int scansize)
    throws BMPException;
}

