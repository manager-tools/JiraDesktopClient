package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.shapes.AbstractShape;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;

/**
 * @author Stalex
 */
public class MultipleShapeStorageLayer implements StorageLayer {

  protected final WorkingImage myImageControl;

  private static final Comparator<AbstractShape> COMPARATOR = new Comparator<AbstractShape>() {
    public int compare(AbstractShape o1, AbstractShape o2) {
      return o2.getOrderID() - o1.getOrderID();
    }
  };

  private static final Comparator<AbstractShape> REV_COMPARATOR = Collections.reverseOrder(COMPARATOR);

  protected final TreeSet<AbstractShape> myShapes = new TreeSet<AbstractShape>(COMPARATOR);


  public MultipleShapeStorageLayer(WorkingImage myImageControl) {
    this.myImageControl = myImageControl;
  }

  public void store(AbstractShape shape) {
    myShapes.add(shape);
  }

  public void storeAll(Collection<AbstractShape> shapes) {
    myShapes.addAll(shapes);
  }

  public void unstore(AbstractShape shape) {
    boolean contains = myShapes.remove (shape);
    assert contains;
  }

  public void unstoreAll(Collection<AbstractShape> shapes) {
    myShapes.removeAll(shapes);
  }

  public AbstractShape getShapeFromRect(Rectangle p) {
    for (AbstractShape as : myShapes) {
      if (as.intersects(p)) {
        return as;
      }
    }
    return null;
  }

  public AbstractShape getShapeFromPoint(Point p) {
    for (AbstractShape as : myShapes) {
      if (as.contains(p)) {
        return as;
      }
    }
    return null;
  }

  @Nullable
  public Rectangle getBounds() {
    return null;
  }

  public void paint(Graphics2D g2, Area clip) {
    Set<AbstractShape> reverse = new TreeSet(REV_COMPARATOR);
    reverse.addAll(myShapes);
    for (AbstractShape shape : reverse) {
      shape.paintShape(g2, clip);
    }
  }
  
  public void resize(Rectangle oldBounds, Rectangle newBounds) {
  }

}
