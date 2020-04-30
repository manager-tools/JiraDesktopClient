package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.shapes.AbstractShape;

import java.awt.*;
import java.util.Collection;

/**
 * @author Stalex
 */
public interface StorageLayer extends Layer {

  void store(AbstractShape shape);

  void storeAll(Collection<AbstractShape> shapes);

  void unstore(AbstractShape shape);

  void unstoreAll(Collection<AbstractShape> shapes);

  AbstractShape getShapeFromPoint(Point p);

  AbstractShape getShapeFromRect(Rectangle p);

}
