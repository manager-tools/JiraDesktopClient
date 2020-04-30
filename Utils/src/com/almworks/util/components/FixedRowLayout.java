package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class FixedRowLayout implements LayoutManager2 {
  public static final String FULL_HEIGHT = "fullHeight";

  private static final int DEFAULT_WIDTH = 100;

  private final boolean myLeftToRight;

  private Integer myMaximumHeight = null;
  private Integer myUsedWidth = null;
  private static final int DEFAULT_HEIGHT = 24;

  public FixedRowLayout(boolean leftToRight) {
    myLeftToRight = leftToRight;
  }

  public void removeLayoutComponent(Component comp) {
    if (comp instanceof JComponent)
      ((JComponent)comp).putClientProperty(FULL_HEIGHT, null);
  }

  public void layoutContainer(Container parent) {
    Dimension size = parent.getSize();
    Component[] components = parent.getComponents();
    int pos = 0;
    int fullHeight = size.height;
    int fullWidth = size.width;
    for (int i = 0; i < components.length; i++) {
      Component component = components[i];
      int width = getWidth(component);
      int height = isFullHeight(component) ? fullHeight : getHeight(component);
      if (myLeftToRight)
        component.setBounds(pos, fullHeight - height, width, height);
      else
        component.setBounds(fullWidth - pos - width, fullHeight - height, width, height);
      pos += width;
    }
    int freeSpace = fullWidth - pos;
    // todo place a component in the free space?
  }

  private boolean isFullHeight(Component component) {
    if (!(component instanceof JComponent))
      return false;
    return ((JComponent)component).getClientProperty(FULL_HEIGHT) != null;
  }

  public void addLayoutComponent(String name, Component comp) {
  }

  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  public Dimension preferredLayoutSize(Container parent) {
    return new Dimension(getUsedWidth(parent), getRowHeight(parent));
  }

  public float getLayoutAlignmentX(Container target) {
    return 0F;
  }

  public float getLayoutAlignmentY(Container target) {
    return 1F;
  }

  public void invalidateLayout(Container target) {
    myMaximumHeight = null;
    myUsedWidth = null;
  }

  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(Integer.MAX_VALUE, getRowHeight(target));
  }

  public void addLayoutComponent(Component comp, Object constraints) {
    if (constraints == FULL_HEIGHT && (comp instanceof JComponent))
      ((JComponent)comp).putClientProperty(FULL_HEIGHT, Boolean.TRUE);
  }

  private int getHeight(Component component) {
    int height = component.getPreferredSize().height;
    if (height < 0)
      height = component.getMinimumSize().height;
    if (height < 0)
      height = component.getMaximumSize().height;
    if (height < 0)
      height = DEFAULT_HEIGHT;
    return height;
  }

  private int getRowHeight(Container target) {
    if (myMaximumHeight == null) {
      Component[] components = target.getComponents();
      int max = 0;
      for (Component component : components) {
        max = Math.max(max, getHeight(component));
      }
      myMaximumHeight = max;
    }
    return myMaximumHeight;
  }

  private int getUsedWidth(Container parent) {
    if (myUsedWidth == null) {
      Component[] components = parent.getComponents();
      int width = 0;
      for (Component component : components) {
        width += getWidth(component);
      }
      myUsedWidth = width;
    }
    return myUsedWidth;
  }

  private int getWidth(Component component) {
    int width = component.getPreferredSize().width;
    if (width < 0)
      width = component.getMinimumSize().width;
    if (width < 0)
      width = component.getMaximumSize().width;
    if (width < 0)
      width = DEFAULT_WIDTH;
    return width;
  }
}
