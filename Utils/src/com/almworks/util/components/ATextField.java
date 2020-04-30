package com.almworks.util.components;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

public class ATextField extends JTextField implements Antialiasable {
  private boolean myAntialiased = false;

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
}
