package com.almworks.util.bmp;

import java.awt.image.ImageProducer;

public interface BMPImage {
  void setFinished() throws BMPException;

  ImageProducer getImageProducer();
}
