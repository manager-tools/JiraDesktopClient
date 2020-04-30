package com.almworks.util.ui;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class IconSizeFixup implements Icon {
  private final Icon myIcon;
  private final int myWidth;
  private final int myHeight;

  public IconSizeFixup(@Nullable Icon icon, int width, int height) {
    myIcon = icon;
    myWidth = width;
    myHeight = height;
  }

  public IconSizeFixup(int width, int height) {
    this(null, width, height);
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    // todo make parameters to align real icon within fixed size
    if (myIcon == null)
      return;
    int w = myIcon.getIconWidth();
    int h = myIcon.getIconHeight();
    boolean needsClip = h > myHeight || w > myWidth;
    if (needsClip) {
      Graphics gg = g.create(x, y, myWidth, myHeight);
      try {
        myIcon.paintIcon(c, gg, 0, 0);
      } finally {
        gg.dispose();
      }
    } else {
      int dy = h >= myHeight ? 0 : (myHeight - h) / 2;
      myIcon.paintIcon(c, g, x, y + dy);
    }
  }

  public int getIconWidth() {
    return myWidth;
  }

  public int getIconHeight() {
    return myHeight;
  }
}
