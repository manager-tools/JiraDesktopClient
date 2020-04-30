package com.almworks.util.components;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelListener;

/**
 * @author dyoma
 */
public class ScrollPaneBorder extends JScrollPane {
  private int myMinHeight = -1;
  private boolean myValidateRoot = true;

  public ScrollPaneBorder(Component view) {
    super(view);
    initScrollPane();
  }

  public Dimension getPreferredSize() {
    Dimension size = priGetPreferedSize();
    size.height = Math.max(myMinHeight, size.height);
    return size;
  }

  public boolean isValidateRoot() {
    return myValidateRoot;
  }

  public void setValidateRoot(boolean validateRoot) {
    myValidateRoot = validateRoot;
  }

  public void setMinHeight(int minHeight) {
    myMinHeight = minHeight;
  }

  private Dimension priGetPreferedSize() {
    if (myValidateRoot)
      return super.getPreferredSize();
    Component view = getViewport().getView();
    if (!(view instanceof JComponent))
      return super.getPreferredSize();
    Dimension size = view.getPreferredSize();
    Insets insets = AwtUtil.uniteInsetsFromTo((JComponent) view, this);
    return AwtUtil.addInsets(size, insets);
  }

  public ScrollPaneBorder() {
    initScrollPane();
  }

  private void initScrollPane() {
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
  }

  public synchronized void addMouseWheelListener(MouseWheelListener l) {
  }

  public void requestFocus() {
    getViewportView().requestFocus();
  }

  public boolean requestFocusInWindow() {
    return getViewportView().requestFocusInWindow();
  }

  private Component getViewportView() {
    return getViewport().getView();
  }
}
