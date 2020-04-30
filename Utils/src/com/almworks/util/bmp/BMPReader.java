package com.almworks.util.bmp;

import java.awt.*;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;


public class BMPReader {
  private final InputStream myStream;
  private final BMPDecoder decoder;

  public BMPReader(InputStream in) {
    myStream = in;
    decoder = new BMPDecoder();
  }

  private Image load() throws IOException, BMPException {
    ImageProducer result;
    PushbackInputStream stream = new PushbackInputStream(myStream, 128);
    decoder.initDecoding(stream);
    result = getImageProducer();
    ImageProducer producer = result;
    return Toolkit.getDefaultToolkit().createImage(producer);
  }

  public ImageProducer getImageProducer() throws BMPException {
    ImageProducer result;
    BMPImage image = asBMPRasterImage(decoder.decode());
    if (image == null)
      throw new BMPException();
    result = image.getImageProducer();
    ImageProducer img = result;
    decoder.setFinished();
    return img;
  }

  public static BMPImage asBMPRasterImage(BMPImage image) throws BMPException {
    if (image instanceof BMPImageProxy)
      return asBMPRasterImage(((BMPImageProxy) image).getWrappedBMPImage());
    else
      return image;
  }


  public static Image loadBMP(InputStream in) throws IOException, BMPException {
    return new BMPReader(in).load();
  }
}
