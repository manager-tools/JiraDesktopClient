package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class ComponentUIUtil {
  public static Dimension calculatePreferedSize(JComponent link, int vAligment, int hAligment, String text) {
    Insets insets = link.getInsets();
    int dx = insets.left + insets.right;
    int dy = insets.top + insets.bottom;
    Rectangle viewR = new Rectangle();
    viewR.x = 0;
    viewR.y = 0;
    viewR.width = Integer.MAX_VALUE;
    viewR.height = Integer.MAX_VALUE;
    Rectangle iconR = new Rectangle();
    Rectangle textR = new Rectangle();
    layoutIconText(link, viewR, iconR, textR, vAligment, hAligment, text);
    int x1 = Math.min(iconR.x, textR.x);
    int x2 = Math.max(iconR.x + iconR.width, textR.x + textR.width);
    int y1 = Math.min(iconR.y, textR.y);
    int y2 = Math.max(iconR.y + iconR.height, textR.y + textR.height);
    Dimension rv = new Dimension(x2 - x1, y2 - y1);
    rv.width += dx;
    rv.height += dy;
    return rv;
  }

  public static String layoutIconText(JComponent link, Rectangle viewR, Rectangle iconR, Rectangle textR, int vAligment,
    int hAligment, String text) {
    if (iconR == null)
      iconR = new Rectangle();
    Insets insets = link.getInsets();
    viewR.x += insets.top;
    viewR.y += insets.left;
    viewR.width -= insets.left + insets.right;
    viewR.height -= insets.top + insets.bottom;
    return SwingUtilities.layoutCompoundLabel(link, link.getFontMetrics(link.getFont()), text, null, vAligment,
      hAligment, SwingUtilities.CENTER, SwingUtilities.CENTER, viewR, iconR, textR, 0);
  }
}
