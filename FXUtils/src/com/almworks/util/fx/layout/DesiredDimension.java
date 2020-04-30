package com.almworks.util.fx.layout;

import javafx.geometry.Insets;
import javafx.scene.Node;

public abstract class DesiredDimension {
  public static final Limit MIN_WIDTH = new Limit(true, true);
  public static final Limit MAX_WIDTH = new Limit(true, false);
  public static final DesiredDimension PREF_WIDTH = new Pref(true, MIN_WIDTH, MAX_WIDTH);
  public static final Limit MIN_HEIGHT = new Limit(false, true);
  public static final Limit MAX_HEIGHT = new Limit(false, false);
  public static final DesiredDimension PREF_HEIGHT = new Pref(false, MIN_HEIGHT, MAX_HEIGHT);

  private final boolean myWidth;

  private DesiredDimension(boolean width) {
    myWidth = width;
  }

  public final boolean isWidth() {
    return myWidth;
  }

  public abstract double get(Node node, double across, SnapToPixel snap);

  public final double sumInsets(Insets insets, SnapToPixel snap) {
    if (insets == null) return 0;
    double size = isWidth() ? insets.getLeft() + insets.getRight() : insets.getTop() + insets.getBottom();
    return snap.size(size);
  }

  public final static class Limit extends DesiredDimension {
    private final boolean myMin;

    public Limit(boolean width, boolean min) {
      super(width);
      myMin = min;
    }

    @Override
    public double get(Node node, double across, SnapToPixel snap) {
      double size;
      if (isWidth()) size = myMin ? node.minWidth(across) : node.maxWidth(across);
      else size = myMin ? node.minHeight(across) : node.maxHeight(across);
      return snap.size(size);
    }

    public boolean exceeds(double size, double limit) {
      if (myMin) return size < limit;
      else return size > limit;
    }

    public double constrain(double size, double limit) {
      if (myMin) return Math.max(size, limit);
      return Math.min(size, limit);
    }
  }

  private final static class Pref extends DesiredDimension {
    private final DesiredDimension myMin;
    private final DesiredDimension myMax;

    public Pref(boolean width, DesiredDimension min, DesiredDimension max) {
      super(width);
      myMin = min;
      myMax = max;
    }

    @Override
    public double get(Node node, double across, SnapToPixel snap) {
      double min = myMin.get(node, -1, snap);
      double max = myMax.get(node, -1, snap);
      double size = isWidth() ? node.prefWidth(across) : node.prefHeight(across);
      size = Math.min(max, Math.max(min, size));
      return snap.size(size);
    }
  }
}
