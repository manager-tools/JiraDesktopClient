package com.almworks.screenshot.editor.image;

import java.awt.*;

public interface ImageComponentControl {
  void setComponentCursor(Cursor cursor);

  void imageResized(Rectangle oldBounds, Rectangle newBounds);

  void requestFocus();

  void toScreenCoordinates(Point p);
}
