package com.almworks.util.components;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class IconBorder implements Border {
  private final Icon myIcon;
  private final int myInnerGap;
  private final int myOuterGap;
  private final int myWidth;

  public IconBorder(Icon icon, int innerGap, int outerGap) {
    myIcon = icon;
    myInnerGap = innerGap;
    myOuterGap = outerGap;
    myWidth = icon.getIconWidth();
  }

  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    int h = myIcon.getIconHeight();
    int dy = (height - h) / 2;
    myIcon.paintIcon(c, g, x + myOuterGap, y + dy);
  }

  public Insets getBorderInsets(Component c) {
    return new Insets(0, myWidth + myInnerGap + myOuterGap, 0, 0);
  }

  public boolean isBorderOpaque() {
    return false;
  }
}
