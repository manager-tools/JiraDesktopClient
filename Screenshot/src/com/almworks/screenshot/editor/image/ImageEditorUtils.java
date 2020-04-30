package com.almworks.screenshot.editor.image;

import java.awt.*;

public class ImageEditorUtils {
  public static final Rectangle EMPTY_BOUNDS = new Rectangle();

  public static void drawRect(Graphics2D g2, Rectangle rect) {
    g2.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);

  }

  public static void drawRect(Graphics2D g2, Rectangle rect, Color color, Stroke stroke) {
    g2.setColor(color);
    g2.setStroke(stroke);
    drawRect(g2, rect);
  }

  public static int getRounding(Rectangle rect) {
    return (int) Math.max(20F, Math.min(((double) rect.width) / 3F, ((double) rect.height) / 3F));
  }

  public static void drawRoundRect(Graphics2D g2, Rectangle rect, int rounding) {
    g2.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, rounding, rounding);
  }

  public static void drawRoundRect(Graphics2D g2, Rectangle rect, Color color, Stroke stroke, int rounding) {
    g2.setColor(color);
    g2.setStroke(stroke);
    drawRoundRect(g2, rect, rounding);
  }


}
