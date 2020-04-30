package com.almworks.util.images;

import javax.swing.*;
import java.awt.*;

public class EmptyIcon implements Icon {
  protected final Icon myBaseIcon;

  public EmptyIcon(Icon baseIcon) {
    myBaseIcon = baseIcon;
  }

  public int getIconHeight() {
    return myBaseIcon.getIconHeight();
  }

  public int getIconWidth() {
    return myBaseIcon.getIconWidth();
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {}
}
