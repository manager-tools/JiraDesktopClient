package com.almworks.util.bmp;

public interface ChanneledIntRasterImage extends IntRasterImage {
  public void setChannelRectangle(int channel, int x, int y, int width, int height, byte[] pixels, int offset,
    int scansize) throws BMPException;
}

