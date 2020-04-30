package com.almworks.util.ui;

import java.awt.*;

public class NoLayout implements LayoutManager2 {
  public static final NoLayout INSTANCE = new NoLayout();

  private NoLayout() {}

  public void addLayoutComponent(Component comp, Object constraints) {
  }

  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(0, 0);
  }

  public float getLayoutAlignmentX(Container target) {
    return 0;
  }

  public float getLayoutAlignmentY(Container target) {
    return 0;
  }

  public void invalidateLayout(Container target) {
  }

  public void addLayoutComponent(String name, Component comp) {
  }

  public void removeLayoutComponent(Component comp) {
  }

  public Dimension preferredLayoutSize(Container parent) {
    return new Dimension(0, 0);
  }

  public Dimension minimumLayoutSize(Container parent) {
    return new Dimension(0, 0);
  }

  public void layoutContainer(Container parent) {
  }
}
