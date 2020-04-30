package com.almworks.util.components;

import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;

/**
 * @author sereda
 */
public class SizeConstraints {
  public static SizeDelegate boundedPreferredSize() {
    return new PreferredSizeBoundedByComponent();
  }

  public static SizeDelegate boundedPreferredSize(Dimension maxSize) {
    return new PreferredSizeBoundedByConstant(null, maxSize);
  }

  public static SizeDelegate preferredMaximumSize(final SizeDelegate delegate) {
    return new SizeDelegate() {
      public Dimension getMinimumSize(JComponent component, Dimension componentSize) {
        return delegate.getMinimumSize(component, componentSize);
      }

      public Dimension getMaximumSize(JComponent component, Dimension componentSize) {
        return getPreferredSize(component, componentSize);
      }

      public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
        return delegate.getPreferredSize(component, componentSize);
      }
    };
  }

  public static abstract class BoundedPreferredSize extends SizeDelegate {
    private static final Dimension MINIMIEST = new Dimension(0, 0);
    private static final Dimension MAXIMIEST = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);

    public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
      return getPreferredSize(componentSize, minBound(component, componentSize), maxBound(component, componentSize));
    }

    protected abstract Dimension maxBound(JComponent component, Dimension componentPreferredSize);

    protected abstract Dimension minBound(JComponent component, Dimension componentPreferredSize);

    private Dimension getPreferredSize(Dimension componentSize, Dimension min, Dimension max) {
      min = Util.NN(min, MINIMIEST);
      max = Util.NN(max, MAXIMIEST);
      int width = Math.min(max.width, Math.max(min.width, componentSize.width));
      int height = Math.min(max.height, Math.max(min.height, componentSize.height));
      return new Dimension(width, height);
    }
  }


  public static class PreferredSizeBoundedByComponent extends BoundedPreferredSize {
    protected Dimension maxBound(JComponent component, Dimension componentPreferredSize) {
      return component.getMaximumSize();
    }

    protected Dimension minBound(JComponent component, Dimension componentPreferredSize) {
      return component.getMinimumSize();
    }
  }


  public static class PreferredSizeBoundedByConstant extends BoundedPreferredSize {
    private final Dimension myMin;
    private final Dimension myMax;

    public PreferredSizeBoundedByConstant(Dimension min, Dimension max) {
      myMin = min;
      myMax = max;
    }

    protected Dimension maxBound(JComponent component, Dimension componentPreferredSize) {
      return myMax;
    }

    protected Dimension minBound(JComponent component, Dimension componentPreferredSize) {
      return myMin;
    }
  }
}
