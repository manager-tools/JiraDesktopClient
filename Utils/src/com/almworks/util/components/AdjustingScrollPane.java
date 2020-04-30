package com.almworks.util.components;

import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class AdjustingScrollPane extends JScrollPane {
  private final int myMinWidth;
  private final int myMaxWidth;
  private final int myMinHeight;
  private final int myMaxHeight;
  private final ComponentAdapter myComponentListener;

  public AdjustingScrollPane(int minColumns, int maxColumns, int minRows, int maxRows) {
    int w = UIUtil.getColumnWidth(this);
    int h = UIUtil.getLineHeight(this);
    myMinWidth = w * minColumns;
    myMaxWidth = w * maxColumns;
    myMinHeight = h * minRows;
    myMaxHeight = h * maxRows;
    myComponentListener = new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        invalidate();
        Container parent = getParent();
        if (parent == null)
          return;
        if (parent instanceof JComponent)
          ((JComponent) parent).revalidate();
      }
    };
  }

  public void addNotify() {
    super.addNotify();
    addComponentListener(myComponentListener);
    myComponentListener.componentResized(null);
  }

  public void removeNotify() {
    removeComponentListener(myComponentListener);
    super.removeNotify();
  }

  public Dimension getPreferredSize() {
    JViewport viewport = getViewport();
    Component view = viewport.getView();
    if (view == null)
      return super.getPreferredSize();
    Dimension size = view.getPreferredSize();
    int width = size.width + AwtUtil.getInsetWidth(viewport) + AwtUtil.getInsetWidth(this);
    width = Math.min(Math.max(width, myMinWidth), myMaxWidth);
    int height = size.height + AwtUtil.getInsetHeight(viewport) + AwtUtil.getInsetHeight(this);
    height = Math.min(Math.max(height, myMinHeight), myMaxHeight);
    JScrollBar bar;
    bar = getHorizontalScrollBar();
    if (bar != null && bar.isVisible())
      height += bar.getSize().height;
    bar = getVerticalScrollBar();
    if (bar != null && bar.isVisible())
      width += bar.getSize().height;
    return new Dimension(width, height);
  }
}
