package com.almworks.screenshot.editor.tools.zoom;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.shapes.DoubleRectShape;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

/**
 * @author Stalex
 */
public class ZoomInShape extends DoubleRectShape {
  private int myTimes = 2;

  public int getTimes() {
    return myTimes;
  }

  private WorkingImage myWorkingImage;

  public ZoomInShape(WorkingImage image, Rectangle rect, int scale, Color color) {
    super(image.getLayerControl(), color);
    myTimes = scale;
    myWorkingImage = image;

    super.setRect(rect);

    setLinkedRect(findLinkedPlace(rect, image.getBounds(), rect.width, rect.height));

    invalidateLinkedRect();

  }


  @Override
  public void paintShape(Graphics2D g2, Area clip) {
    super.paintShape(g2, clip);
    BufferedImage source = myWorkingImage.getInitialSubImage(getRect());
    Rectangle linkedRect = getLinkedRect();
    g2.drawImage(source, linkedRect.x, linkedRect.y, linkedRect.width, linkedRect.height, null);
    Rectangle sourceRect = getRect();
    g2.drawImage(source, sourceRect.x, sourceRect.y, sourceRect.width, sourceRect.height, null);

    ImageEditorUtils.drawRect(g2, sourceRect, getColor(), myBasic);
    ImageEditorUtils.drawRect(g2, linkedRect, getColor(), myBasic);
  }

  @Override
  public void setRect(Rectangle rect) {
    super.setRect(rect);
    invalidateLinkedRect();
  }

  protected void invalidateLinkedRect() {
    Rectangle rect = getLinkedRect();
    rect.setSize(getRect().width * myTimes, getRect().height * myTimes);
    setLinkedRect(rect);

  }

  public void setScale(int value) {
    myTimes = value;
    invalidateLinkedRect();
  }

}
