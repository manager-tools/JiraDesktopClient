package com.almworks.screenshot.editor.shapes;

import com.almworks.screenshot.editor.layers.LayerImageControl;

import java.awt.*;
import java.awt.geom.Area;

/**
 * @author Stalex
 */
public abstract class DoubleRectShape extends AbstractShape {

  private Rectangle myLinkedRect;

  protected Stroke myBasic = new BasicStroke(1);

  protected DoubleRectShape(LayerImageControl myImageControl, Color color) {
    super(myImageControl);
    setColor(color);
  }

  @Override
  public boolean contains(Point point) {
    return (myLinkedRect.contains(point)) || (super.contains(point));
  }

    @Override
  public boolean intersects(Rectangle p) {
    return (myLinkedRect.intersects(p)) || (super.intersects(p));
  }

  public Object clone() {
    Object o = super.clone();
    myLinkedRect = new Rectangle(myLinkedRect);
    return o;
  }

  @Override
  public void translate(int dx, int dy) {
    super.translate(dx, dy);
    myLinkedRect.translate(dx, dy);
  }

  public boolean equals(Object obj) {
    return (obj != null ) && (obj instanceof DoubleRectShape)
      && super.equals(obj) && ((DoubleRectShape) obj).myLinkedRect.equals(myLinkedRect) ;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 37 * myLinkedRect.hashCode() + result;
    return result;
  }

  public Rectangle getLinkedRect() {
    return new Rectangle(myLinkedRect);
  }

  public Rectangle getBounds() {
    Rectangle rectangle = getRect().union(myLinkedRect);
    rectangle.grow(1,1);
    return rectangle;
  }

  public void paintShape(Graphics2D g2, Area clip) {
    paintLink(g2, getColor());
  }

  protected void paintLink(Graphics2D g2, Color color) {
    g2.setStroke(myBasic);
    g2.setColor(color);
    Rectangle rect = getRect();
    g2.drawLine((int) rect.getCenterX(), (int) rect.getCenterY(), (int) myLinkedRect.getCenterX(), (int) myLinkedRect.getCenterY());
  }

  public void setLinkedRect(Rectangle runningSelection) {
    myLinkedRect = new Rectangle(runningSelection);
  }


  protected static Rectangle findLinkedPlace(Rectangle bounds, Rectangle contBounds, int minWidth, int minHeight) {
    int xPos;
    int yPos;

    if (contBounds.getMaxX() - bounds.getMaxX() > minWidth) {
      xPos = (int) bounds.getMaxX() + 5;
      if (contBounds.getMaxY() - bounds.getMaxY() > minHeight) {
        yPos = (int) bounds.getMaxY() + 5;
      } else {
        yPos = bounds.y - minWidth - 5;
      }
    } else /*if (bounds.getMinX() - contBounds.getMinX() > MIN_WIDTH )*/ {
      xPos = (int) bounds.getMinX() - minWidth - 5;
      if (contBounds.getMaxY() - bounds.getMaxY() > minHeight) {
        yPos = (int) bounds.getMaxY() + 5;
      } else {
        yPos = bounds.y - minHeight - 5;
      }
    }


    return new Rectangle(xPos, yPos, minWidth, minHeight);

  }
}
