package com.almworks.util.ui;

import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class BrokenLineBorder extends AbstractBorder {
  public static final int DOTTED = 10002;
  public static final int EAST = 2;
  public static final int NORTH = 1;
  public static final int SOLID = 10001;
  public static final int SOUTH = 4;
  public static final int WEST = 8;

  protected final int thickness;
  protected Color lineColor;
  protected int sides;
  private int myStyle = SOLID;

  public BrokenLineBorder(Color color, int thickness, int sides, int style) {
    this.lineColor = color;
    this.thickness = thickness;
    this.sides = sides;
    myStyle = style;
  }

  public BrokenLineBorder(Color color, int thickness, int sides) {
    this(color, thickness, sides, SOLID);
  }

  /**
   * Reinitialize the insets parameter with this Border's current Insets.
   *
   * @param c      the component for which this border insets value applies
   * @param insets the object to be reinitialized
   */
  public Insets getBorderInsets(Component c, Insets insets) {
    insets.top = hasSide(NORTH) ? thickness : 0;
    insets.right = hasSide(EAST) ? thickness : 0;
    insets.bottom = hasSide(SOUTH) ? thickness : 0;
    insets.left = hasSide(WEST) ? thickness : 0;
    return insets;
  }

  /**
   * Returns the insets of the border.
   *
   * @param c the component for which this border insets value applies
   */
  public Insets getBorderInsets(Component c) {
    return getBorderInsets(c, new Insets(0, 0, 0, 0));
  }

  /**
   * Returns the color of the border.
   */
  public Color getLineColor() {
    return lineColor;
  }

  /**
   * Returns whether this border will be drawn with rounded corners.
   */
  public int getSides() {
    return sides;
  }

  /**
   * Returns the thickness of the border.
   */
  public int getThickness() {
    return thickness;
  }


  public boolean hasSide(int side) {
    return side > 0 && ((sides & side) == side);
  }

  /**
   * Paints the border for the specified component with the
   * specified position and size.
   *
   * @param c      the component for which this border is being painted
   * @param g      the paint graphics
   * @param x      the x position of the painted border
   * @param y      the y position of the painted border
   * @param width  the width of the painted border
   * @param height the height of the painted border
   */
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Color oldColor = g.getColor();
    g.setColor(lineColor);
    for (int i = 0; i < thickness; i++) {
      int left = x + i;
      int top = y + i;
      int right = x + width - i - 1;
      int bottom = y + height - i - 1;
      if (hasSide(NORTH))
        drawHLine(g, left - i, right + i, top, myStyle);
      if (hasSide(EAST))
        drawVLine(g, right, top - i, bottom + i, myStyle);
      if (hasSide(SOUTH))
        drawHLine(g, left - i, right + i, bottom, myStyle);
      if (hasSide(WEST))
        drawVLine(g, left, top - i, bottom + i, myStyle);
    }
    g.setColor(oldColor);
  }

  public static void drawHLine(Graphics g, int x1, int x2, int y, int style) {
    if (style == SOLID) {
      g.drawLine(x1, y, x2, y);
    } else if (style == DOTTED) {
      for (int x = x1; x < x2; x += 3)
        g.drawLine(x, y, x, y);
    } else
      throw new IllegalArgumentException("unknown style " + style);
  }

  public static void drawVLine(Graphics g, int x, int y1, int y2, int style) {
    if (style == SOLID) {
      g.drawLine(x, y1, x, y2);
    } else if (style == DOTTED) {
      for (int y = y1; y < y2; y += 3)
        g.drawLine(x, y, x, y);
    } else
      throw new IllegalArgumentException("unknown style " + style);
  }
}
