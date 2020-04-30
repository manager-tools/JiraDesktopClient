package com.almworks.screenshot;

import com.almworks.api.screenshot.Screenshot;

import java.awt.image.BufferedImage;

public class ValidScreenshot implements Screenshot {
  private final BufferedImage myImage;
  private final String myAttachmentName;

  public ValidScreenshot(BufferedImage image, String attachmentName) {
    myImage = image;
    myAttachmentName = attachmentName;
  }

  public BufferedImage getImage() {
    return myImage;
  }

  public String getAttachmentName() {
    return myAttachmentName;
  }
}
