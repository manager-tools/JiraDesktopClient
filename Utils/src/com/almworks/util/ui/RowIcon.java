package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author dyoma
 */
public class RowIcon implements Icon {
  private final Icon[] myIcons;
  private final int myHeight;
  private final int myWidth;
  private final int myDefaultWidth;
  private final int myGap;
  private final int myLeftMargin;

  private RowIcon(Icon[] icons, int defaultWidth, int defaultHeight, int gap, int leftMargin, int rightMargin) {
    assert checkIcons(icons);
    myLeftMargin = leftMargin;
    myIcons = icons;
    myGap = gap;
    myHeight = getHeight(icons, defaultHeight);
    myDefaultWidth = defaultWidth;
    myWidth = getWidth(icons, myDefaultWidth, gap, leftMargin, rightMargin);
  }

  public static RowIcon create(Icon ... icons) {
    return new RowIcon(icons, 0, 0, 0, 0, 0);
  }

  public static RowIcon create(int defaultWidth, int defaultHeight, int gap, int leftMargin, int rightMargin, Icon ... icons) {
    return new RowIcon(icons, defaultWidth, defaultHeight, gap, leftMargin, rightMargin);
  }

  public static RowIcon create(int defaultWidth, int defaultHeight, Collection<? extends Icon> icons) {
    return new RowIcon(icons.toArray(new Icon[icons.size()]), defaultWidth, defaultHeight, 0, 0, 0);
  }

  public static RowIcon create(int defaultWidth, int defaultHeight, int gap, int leftMargin, int rightMargin, Collection<? extends Icon> icons) {
    return new RowIcon(icons.toArray(new Icon[icons.size()]), defaultWidth, defaultHeight, gap, leftMargin, rightMargin);
  }

  public int getIconIndex(int x) {
    x -= myLeftMargin;
    for (int i = 0; i < myIcons.length; i++) {
      if (x < 0) return -1;
      Icon icon = myIcons[i];
      int iconWidth = icon != null ? icon.getIconWidth() : myDefaultWidth;
      if (x < iconWidth) return icon != null ? i : -1;
      x -= iconWidth + myGap;
    }
    return -1;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    int cx = x + myLeftMargin;
    for (Icon icon : myIcons) {
      int iconWidth;
      if (icon == null) iconWidth = myDefaultWidth;
      else {
        iconWidth = icon.getIconWidth();
        int h = icon.getIconHeight();
        int dy = (myHeight - h) /  2;
        icon.paintIcon(c, g, cx, y + dy);
      }
      cx += iconWidth + myGap;
    }
  }

  public int getIconWidth() {
    return myWidth;
  }

  public int getIconHeight() {
    return myHeight;
  }

  private static boolean checkIcons(Icon[] icons) {
    assert icons != null;
    assert icons.length > 0;
    for (Icon icon : icons) {
      if (icon == null) continue;
      int height = icon.getIconHeight();
      int width = icon.getIconWidth();
      assert height > 0 :icon;
      assert width > 0 :icon;
    }
    return true;
  }

  private static int getHeight(Icon[] icons, int defaultHeight) {
    int height = 0;
    for (Icon icon : icons) {
      int h = icon != null ? icon.getIconHeight() :defaultHeight;
      height = Math.max(h, height);
    }
    return height;
  }

  private static int getWidth(Icon[] icons, int defaultWidth, int gap, int left, int right) {
    int width = left + right;
    if (icons.length == 0) return width;
    for (Icon icon : icons) {
      int w = icon != null ? icon.getIconWidth() : defaultWidth;
      width += w;
    }
    return width + gap *(icons.length - 1);
  }
}
