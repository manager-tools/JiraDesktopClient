package com.almworks.util.components;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class ATextArea extends JTextArea implements Antialiasable{
  private boolean myAntialiased = false;
  private Boolean myTracksViewportWidth = null;
  private Boolean myTracksViewportHeight = null;

  public void setAntialiased(boolean antialiased) {
    myAntialiased = antialiased;
    repaint();
  }

  public boolean isAntialiased() {
    return myAntialiased;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
  }

  public void trackViewportDimensions() {
    myTracksViewportWidth = Boolean.TRUE;
    myTracksViewportHeight = Boolean.TRUE;
  }

  public void trackViewportHeight() {
    myTracksViewportHeight = true;
  }

  public boolean getScrollableTracksViewportWidth() {
    return myTracksViewportWidth != null ? myTracksViewportWidth : super.getScrollableTracksViewportWidth();
  }

  public boolean getScrollableTracksViewportHeight() {
    return myTracksViewportHeight != null ? myTracksViewportHeight : super.getScrollableTracksViewportHeight();
  }
}
