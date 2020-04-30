package com.almworks.screenshot.editor.image;

import java.awt.*;
import java.awt.geom.Area;

public class DirtyRegionsCollectorImpl implements DirtyRegionsCollector {
  private final Area myDirty = new Area();

  public void dirty(Shape shape) {
    myDirty.add(new Area(shape));
  }

  public Area beginPaint(Rectangle imageClip) {
    Area area = (Area) myDirty.clone();
    area.intersect(new Area(imageClip));
    return area;
  }

  public void endPaint(Area paintArea) {
    myDirty.subtract(paintArea);
  }

  public void clear() {
    myDirty.intersect(new Area());
  }
}
