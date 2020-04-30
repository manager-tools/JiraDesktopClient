package com.almworks.screenshot.editor.tools.transform;

import com.almworks.screenshot.editor.layers.LayerImageControl;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import java.awt.*;
import java.awt.geom.Area;


public class SelectionShape extends AbstractShape {

  private AbstractShape myShape;

  public AbstractShape getShape() {
    return myShape;
  }

  public void setShape(AbstractShape shape) {
    myShape = shape;
  }

  public AbstractShape getClone() {
    assert myShape != null;

    return (AbstractShape) myShape.clone();
  }

  protected SelectionShape(LayerImageControl myImageControl, AbstractShape shape) {
    super(myImageControl);
    setRect(shape.getBounds());
    myShape = shape;
  }

  public void paintShape(Graphics2D g2, Area clip) {

  }

  public void translate(int dx, int dy) {
    super.translate(dx, dy);
    assert myShape != null;
    myShape.translate(dx, dy);
  }
}
