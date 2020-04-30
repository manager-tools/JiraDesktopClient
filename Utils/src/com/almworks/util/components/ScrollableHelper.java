package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

public class ScrollableHelper {
  public static JViewport getParentViewport(Component c) {
    if (c == null)
      return null;
    Container parent = c.getParent();
    if (parent == null)
      return null;
    if (!(parent instanceof JViewport))
      return null;
    return (JViewport) parent;
  }

  public static Dimension getPreferredScrollableSize(Component c) {
    Dimension preferred = null;
    if (c instanceof Scrollable)
      preferred = ((Scrollable)c).getPreferredScrollableViewportSize();
    if (preferred == null)
      preferred = c.getPreferredSize();
    return preferred;
  }

  public static boolean getScrollableTracksViewportHeightStandard(Component c) {
    JViewport viewport = getParentViewport(c);
    if (viewport == null)
      return false;
    Dimension preferred = getPreferredScrollableSize(c);
    return preferred == null ? true : viewport.getHeight() > preferred.height;
  }
  public static boolean getScrollableTracksViewportWidthStandard(Component c) {
    JViewport viewport = getParentViewport(c);
    if (viewport == null)
      return false;
    Dimension preferred = getPreferredScrollableSize(c);
    return preferred == null ? true : viewport.getWidth() > preferred.width;
  }

  public static int getScrollableBlockIncrementStandard(int orientation, Rectangle visibleRect) {
    switch (orientation) {
    case SwingConstants.VERTICAL:
      return visibleRect.height;
    case SwingConstants.HORIZONTAL:
      return visibleRect.width;
    default:
      throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }
  }

  public static int getScrollableUnitIncrementStandard(int orientation, Rectangle visibleRect) {
    switch (orientation) {
    case SwingConstants.VERTICAL:
      return visibleRect.height / 10;
    case SwingConstants.HORIZONTAL:
      return visibleRect.width / 10;
    default:
      throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }
  }
}
