package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author Dyoma
 */
public class EmptyIcon implements Icon {
  private final int myWidth;
  private final int myHeight;
  public static final Icon ZERO_SIZE = new EmptyIcon(0, 0);

  public EmptyIcon(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {}

  public int getIconWidth() {
    return myWidth;
  }

  public int getIconHeight() {
    return myHeight;
  }

  public static Icon sameSize(Icon original) {
    return new EmptyIcon(original.getIconWidth(), original.getIconHeight());
  }
}
