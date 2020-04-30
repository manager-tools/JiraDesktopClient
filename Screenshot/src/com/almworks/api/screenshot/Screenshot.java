package com.almworks.api.screenshot;

import java.awt.image.BufferedImage;

public interface Screenshot {
  BufferedImage getImage();

  String getAttachmentName();
}
