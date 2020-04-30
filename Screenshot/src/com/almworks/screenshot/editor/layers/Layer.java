package com.almworks.screenshot.editor.layers;

import java.awt.*;
import java.awt.geom.Area;

public interface Layer {

  Rectangle getBounds();

  void paint(Graphics2D g2, Area clip);

  void resize(Rectangle oldBounds, Rectangle newBounds);
}
