package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

public class ScrollablePanel extends JPanel implements Scrollable, ScrollableAware {
  private JComponent myComponent;

  public ScrollablePanel(JComponent component) {
    super(new BorderLayout());
    super.addImpl(component, null, -1);
    myComponent = component;
  }

  public static ScrollablePanel adapt(JComponent component) {
    return new ScrollablePanel.ComponentAdapter(component);
  }

  public static ScrollablePanel create(JComponent component) {
    return new ScrollablePanel(component);
  }

  protected void addImpl(Component comp, Object constraints, int index) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void reshape(int x, int y, int w, int h) {
    super.reshape(x, y, w, h);
  }

  public boolean getScrollableTracksViewportHeight() {
    Container parent = getParent();
    if (parent == null)
      return false;
    if (!(parent instanceof JViewport))
      return false;
    Dimension viewport = ((JViewport)parent).getSize();
    Dimension preferred = getPreferredScrollableViewportSize();
    if (viewport == null || preferred == null)
      return false;

    ScrollableAware scrollableAware = ScrollableAware.COMPONENT_PROPERTY.getClientValue(myComponent);
    if (scrollableAware == null)
      scrollableAware = this;
    return scrollableAware.wantFillViewportHeight(viewport, preferred);
  }

  public boolean wantFillViewportHeight(Dimension viewport, Dimension preferred) {
    return viewport.height > preferred.height;
  }

  public boolean getScrollableTracksViewportWidth() {
    Container parent = getParent();
    if (parent == null)
      return false;
    if (!(parent instanceof JViewport))
      return false;
    Dimension viewport = ((JViewport)parent).getSize();
    Dimension preferred = getPreferredScrollableViewportSize();
    if (viewport == null || preferred == null)
      return false;

    ScrollableAware scrollableAware = ScrollableAware.COMPONENT_PROPERTY.getClientValue(myComponent);
    if (scrollableAware == null)
      scrollableAware = this;
    return scrollableAware.wantFillViewportWidth(viewport, preferred);
  }

  public boolean wantFillViewportWidth(Dimension viewport, Dimension preferred) {
    return viewport.width > preferred.width;
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getStdBlockIncrement(orientation, visibleRect);
  }

  public static int getStdBlockIncrement(int orientation, Rectangle visibleRect) {
    switch (orientation) {
    case SwingConstants.VERTICAL:
      return visibleRect.height;
    case SwingConstants.HORIZONTAL:
      return visibleRect.width;
    default:
      throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getStdUnitIncrement(orientation, visibleRect);
  }

  public static int getStdUnitIncrement(int orientation, Rectangle visibleRect) {
    switch (orientation) {
    case SwingConstants.VERTICAL:
      return visibleRect.height / 10;
    case SwingConstants.HORIZONTAL:
      return visibleRect.width / 10;
    default:
      throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }
  }

  public static class ComponentAdapter extends ScrollablePanel {
    public ComponentAdapter(JComponent component) {
      super(component);
    }

    public boolean getScrollableTracksViewportHeight() {
      return true;
    }

    public boolean getScrollableTracksViewportWidth() {
      return true;
    }
  }
}
