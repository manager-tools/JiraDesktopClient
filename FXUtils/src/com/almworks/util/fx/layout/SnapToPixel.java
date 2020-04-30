package com.almworks.util.fx.layout;

import javafx.scene.layout.Region;

public interface SnapToPixel {
  SnapToPixel DO_NOTHING = new SnapToPixel() {
    @Override
    public double size(double size) {
      return size;
    }

    @Override
    public double change(double change) {
      return change;
    }

    @Override
    public String toString() {
      return "Do-not-snap";
    }
  };
  SnapToPixel SNAP = new SnapToPixel() {
    @Override
    public double size(double size) {
      return Math.ceil(size);
    }

    @Override
    public double change(double change) {
      if (change > 0) return Math.floor(change);
      return Math.ceil(change);
    }

    @Override
    public String toString() {
      return "Snap-to-pixel";
    }
  };
  static SnapToPixel fromRegion(Region region) {
    return region != null && region.isSnapToPixel() ? SNAP : DO_NOTHING;
  }

  double size(double size);

  double change(double change);
}
