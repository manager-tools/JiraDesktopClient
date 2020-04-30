package com.almworks.screenshot.editor.tools.highlight;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.layers.LayerImageControl;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import java.awt.*;
import java.awt.geom.Area;

/**
 * @author Stalex
 */
public class HighlightShape extends AbstractShape {

  protected int myRounding = 0;

  public static final int STROKE_WIDTH = 3;
  public static final Stroke NOTE_AREA_STROKE = new BasicStroke(STROKE_WIDTH);

  public HighlightShape(LayerImageControl imageControl, Rectangle rect, Color color) {
    super(imageControl);
    setRect(rect);
    setColor(color);
  }

  @Override
  public Rectangle getBounds() {
    int growParam = STROKE_WIDTH / 2 + 1;
    final Rectangle rectangle = getRect();
    rectangle.grow(growParam, growParam);
    return rectangle; 
  }

  public void paintShape(Graphics2D g2, Area clip) {
    ImageEditorUtils.drawRoundRect(g2, getRect(), getColor(), NOTE_AREA_STROKE, ImageEditorUtils.getRounding(getRect()));
  }
 
}