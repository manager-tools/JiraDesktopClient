package com.almworks.screenshot.editor.image;

import java.awt.*;

public interface RepaintSink {
  void requestRepaint(Rectangle bounds);
}
