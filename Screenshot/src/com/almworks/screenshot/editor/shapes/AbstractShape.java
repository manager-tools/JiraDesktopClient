package com.almworks.screenshot.editor.shapes;

import com.almworks.screenshot.editor.layers.LayerImageControl;
import org.almworks.util.Log;

import java.awt.*;
import java.awt.geom.Area;

/**
 * @author Stalex
 *
 * Every visible object on canvas is shape. This class is
 * base for every visible shape.
 * Every object have id number, which is used for comparing
 * objects. As shapes are stored in treeSet, and for undo/redo
 * implementation we need to create clones, id number used for
 * identificating, clones
 * OrderID is used for sorting treeSet, and for ordering shapes. When shape
 * gets selection, new orderID is proveded, so every "touched" object comes to the surface
 * 
 *
 */
public abstract class AbstractShape implements Cloneable {

  private static int orderCouter = 0;
  private static int idCouter = 0;

  protected LayerImageControl myImageControl;

  // never could be null
  private Rectangle myRect;

  private Color myColor;

  protected int myOrderID = getNextOrderNumber();

  protected int myID = getNewID();

  private static int getNextOrderNumber() {
    return orderCouter++;
  }

  private static int getNewID() {
    return idCouter++;
  }

  protected AbstractShape(LayerImageControl imageControl) {
    myImageControl = imageControl;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null ) && (obj instanceof AbstractShape)
        && ((AbstractShape) obj).myID == myID &&
        ((AbstractShape) obj).myRect.equals(myRect) ;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * myID + result;
    result = 37 * myRect.hashCode() + result;
    return result;
  }

  @Override
  public Object clone() {
    try {
      Object o = super.clone();
      ((AbstractShape) o).myOrderID = getNextOrderNumber();
      myRect = new Rectangle(myRect);
      return o;

    } catch (CloneNotSupportedException e) {
      Log.warn(e);
      return null;
    }
  }

  public abstract void paintShape(Graphics2D g2, Area clip);

  public void setRect(Rectangle rect) {
    myRect = rect;
  }

  public Rectangle getRect() {
    return new Rectangle(myRect);
  }

  public void translate(int dx, int dy) {
    myRect.translate(dx, dy);
  }

  public Rectangle getBounds() {
    return getRect();
  }

 public int getOrderID() {
    return myOrderID;
  }


  public boolean contains(Point point) {
    return myRect.contains(point);
  }

  public boolean intersects(Rectangle p) {
    return myRect.intersects(p);
  }

  public Color getColor() {
    return myColor;
  }

  public void setColor(Color color) {
    myColor = color;
  }
}
