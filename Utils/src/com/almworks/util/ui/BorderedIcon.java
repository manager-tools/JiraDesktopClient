package com.almworks.util.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BorderedIcon implements Icon {
  private final Icon myIcon;
  private final Border myBorder;
  private static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);

  public BorderedIcon(Icon icon, Border border) {
    assert icon != null;
    myIcon = icon;
    myBorder = border == null ? EMPTY_BORDER : border;
    checkBorderTolerance();
  }

  /**
   * Checks if border can live without component.
   */
  private void checkBorderTolerance() {
    myBorder.getBorderInsets(null);
    BufferedImage image = new BufferedImage(100, 20, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics g = image.getGraphics();
    try {
      myBorder.paintBorder(null, g, 0, 0, 100, 20);
    } finally {
      g.dispose();
    }
  }

  public int getIconHeight() {
    Insets insets = myBorder.getBorderInsets(null);
    return getHeight(insets);
  }

  private int getHeight(Insets insets) {
    return myIcon.getIconHeight() + insets.top + insets.bottom;
  }

  public int getIconWidth() {
    Insets insets = myBorder.getBorderInsets(null);
    return getWidth(insets);
  }

  private int getWidth(Insets insets) {
    return myIcon.getIconHeight() + insets.left + insets.right;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    Insets insets = myBorder.getBorderInsets(null);
    myBorder.paintBorder(null, g, x, y, getWidth(insets), getHeight(insets));
    myIcon.paintIcon(c, g, x + insets.left, y + insets.top);
  }
}
