package com.almworks.util.components.renderer;

import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ShadingComponent extends BaseRendererComponent {
  private Paint myPaint = null;
  private Color myCachedForeground = null;

  public ShadingComponent() {
    setOpaque(true);
  }

  protected void paintComponent(Graphics g) {
    Graphics2D graphics = (Graphics2D) g.create();
    try {
      AwtUtil.applyRenderingHints(graphics);
      graphics.setPaint(getBackground());
      graphics.fillRect(0, 0, getWidth(), getHeight());
      graphics.setPaint(getPaint());
      graphics.fillRect(0, 0, getWidth(), getHeight());
    } finally {
      graphics.dispose();
    }
  }

  private Paint getPaint() {
    Color foreground = getForeground();
    if (myPaint != null && Util.equals(myCachedForeground, foreground))
      return myPaint;
    myCachedForeground = foreground;
    myPaint = createShadingPaint(null, foreground, 6);
    return myPaint;
  }

  public static TexturePaint createShadingPaint(Color bgColor, Color lineColor, int step) {
    int size = step < 50 ? ((int) (101 / step)) * step : step * 2;
    Rectangle rect = new Rectangle(0, 0, size, size);
    BufferedImage image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = (Graphics2D) image.getGraphics();
    try {
      if (bgColor != null) {
        g2.setColor(bgColor);
        g2.fill(rect);
      }
      g2.setColor(lineColor);
      int h = rect.height;
      int maxx = rect.width + h;
      for (int x = -h; x <= maxx; x += step) {
        g2.drawLine(x, 0, x + h, h);
        g2.drawLine(x, 0, x - h, h);
      }
    } finally {
      g2.dispose();
    }
    return new TexturePaint(image, rect);
  }
}
