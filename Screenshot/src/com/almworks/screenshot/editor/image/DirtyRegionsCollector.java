package com.almworks.screenshot.editor.image;

import java.awt.*;

public interface DirtyRegionsCollector {
  void dirty(Shape shape);
}
