package com.almworks.screenshot.shooter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Stalex
 */
public interface Capturer {
  BufferedImage capture(GraphicsDevice device);
}
