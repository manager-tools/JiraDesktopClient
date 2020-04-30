package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

/**
 * todo make one-dimension size delegate and collection of three delegates to avoid code repetition
 *
 * @author sereda
 */
public abstract class SizeDelegate {
  public Dimension getMinimumSize(JComponent component, Dimension componentSize) {
    return componentSize;
  }

  public Dimension getMaximumSize(JComponent component, Dimension componentSize) {
    return componentSize;
  }

  public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
    return componentSize;
  }

  public static Dimension maximum(JComponent component, SizeDelegate delegate, Dimension size) {
    if (delegate != null) {
      Dimension r = delegate.getMaximumSize(component, size);
      if (r != null)
        size = r;
    }
    return size;
  }

  public static Dimension minimum(JComponent component, SizeDelegate delegate, Dimension size) {
    if (delegate != null) {
      Dimension r = delegate.getMinimumSize(component, size);
      if (r != null)
        size = r;
    }
    return size;
  }

  public static Dimension preferred(JComponent component, SizeDelegate delegate, Dimension size) {
    if (delegate != null) {
      Dimension r = delegate.getPreferredSize(component, size);
      if (r != null)
        size = r;
    }
    return size;
  }

  public static abstract class SingleFunction extends SizeDelegate {
    public abstract Dimension getSize(JComponent component, Dimension componentSize);

    public Dimension getMaximumSize(JComponent t, Dimension componentSize) {
      Dimension size = getSize(t, componentSize);
      return size == null ? componentSize : size;
    }

    public Dimension getMinimumSize(JComponent t, Dimension componentSize) {
      Dimension size = getSize(t, componentSize);
      return size == null ? componentSize : size;
    }

    public Dimension getPreferredSize(JComponent t, Dimension componentSize) {
      Dimension size = getSize(t, componentSize);
      return size == null ? componentSize : size;
    }
  }

  public static class Fixed extends SizeDelegate {
    private Integer myMinWidth = null;
    private Integer myMinHeight = null;
    private Integer myPrefWidth = null;
    private Integer myPrefHeight = null;
    private Integer myMaxWidth = null;
    private Integer myMaxHeight = null;

    public Fixed setMinWidth(Integer minWidth) {
      myMinWidth = minWidth;
      return this;
    }

    public Fixed setMinHeight(Integer minHeight) {
      myMinHeight = minHeight;
      return this;
    }

    public Fixed setPrefWidth(Integer prefWidth) {
      myPrefWidth = prefWidth;
      return this;
    }

    public Fixed setPrefHeight(Integer prefHeight) {
      myPrefHeight = prefHeight;
      return this;
    }

    public Fixed setMaxWidth(Integer maxWidth) {
      myMaxWidth = maxWidth;
      return this;
    }

    public Fixed setMaxHeight(Integer maxHeight) {
      myMaxHeight = maxHeight;
      return this;
    }

    @Override
    public Dimension getMinimumSize(JComponent component, Dimension componentSize) {
      return apply(componentSize, myMinWidth, myMinHeight);
    }

    @Override
    public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
      return apply(componentSize, myPrefWidth, myPrefHeight);
    }

    private Dimension apply(Dimension componentSize, Integer width, Integer height) {
      if (myMaxWidth != null && width != null) width = Math.min(myMaxWidth, width);
      if (myMaxHeight != null && height != null) height = Math.min(myMaxHeight, height);
      if (width != null) componentSize.width = width;
      if (height != null) componentSize.height = height;
      return componentSize;
    }
  }
}
