package com.almworks.screenshot.editor.tools.blur;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author
 */
public interface Filter {
  public BufferedImage filter(BufferedImage srcImage, Rectangle rect, int weight);
}
