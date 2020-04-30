package com.almworks.screenshot.editor.layers;

import org.almworks.util.Util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

/**
 * @author Alex
 */

public class WatermarkLayer implements Layer {
  private static final Color COLOR = new Color(0, 0, 0, 10);

  private final String myWatermarkText;
  private Rectangle myBounds;

  private Font myFont;
  private AffineTransform myTransform;
  private Point myPoint;

  public WatermarkLayer(String watermark) {
    myWatermarkText = watermark;
  }

  public Rectangle getBounds() {
    return myBounds;
  }

  public void paint(Graphics2D g2, Area clip) {
    if (myBounds == null || myBounds.width == 0 || myBounds.height == 0) {
      return;
    }
    Graphics2D gg = (Graphics2D) g2.create();
    validate(g2);
    if (myFont == null || myPoint == null || myTransform == null)
      return;
    gg.translate(myBounds.x, myBounds.y);
    gg.transform(myTransform);
    gg.setFont(myFont);
    gg.setColor(COLOR);
    gg.drawString(myWatermarkText, myPoint.x, myPoint.y);
    gg.dispose();
  }

  private void validate(Graphics2D g) {
    if (myFont != null && myPoint != null && myTransform != null)
      return;
    double diag = Math.sqrt(myBounds.width * myBounds.width + myBounds.height * myBounds.height);
    int initSize = 32;
    Rectangle2D bounds;
    Font font = new Font("Arial", Font.BOLD, initSize);
    bounds = g.getFontMetrics(font).getStringBounds(myWatermarkText, g);
    double size = initSize * (diag / Math.max(1, bounds.getWidth())) * 5 / 9;
    font = new Font("Arial", Font.BOLD, (int) size);
    bounds = g.getFontMetrics(font).getStringBounds(myWatermarkText, g);
    if (bounds.getHeight() * 1.5 > myBounds.height) {
      size = size * myBounds.height / bounds.getHeight() / 1.5;
      font = new Font("Arial", Font.BOLD, (int) size);
      bounds = g.getFontMetrics(font).getStringBounds(myWatermarkText, g);
    }
    assert bounds.getHeight() * 1.5 < myBounds.height + 1;
    myFont = font;

    double theta = Math.atan2(myBounds.height - bounds.getHeight(), myBounds.width);
    if (Double.isNaN(theta))
      theta = 0.0;
    myTransform = AffineTransform.getRotateInstance(theta);
    myPoint = new Point((int) ((diag - bounds.getWidth()) / 2), g.getFontMetrics(myFont).getAscent());
  }

  public void resize(Rectangle oldBounds, Rectangle newBounds) {
    if (Util.equals(newBounds, oldBounds))
      return;
    myBounds = newBounds;
    myFont = null;
    myTransform = null;
    myPoint = null;
  }
}
