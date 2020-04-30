package com.almworks.screenshot.editor.tools.highlight;

import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.MultipleShapeStorageLayer;
import com.almworks.screenshot.editor.layers.SingleShapeActionLayer;
import com.almworks.screenshot.editor.layers.StorageLayer;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import java.awt.*;

/**
 * @author Stalex
 */
public class HighlightActionLayer extends SingleShapeActionLayer<HighlightShape, SingleShapeActionLayer.LayerColorOptions> {

  public HighlightActionLayer(WorkingImage imageControl) {
    super(imageControl);
    myOptions = new LayerColorOptions("Emphasize", "Click and drag to select an area you'd like to emphasize.");
  }

  /*public HighlightShape createShape(HighlightShape highlightShape) {
    return new HighlightShape(highlightShape);
  }
*/
  public HighlightShape createShape(Rectangle shape) {
    return new HighlightShape(myWorkingImage, shape, myOptions.getColor());
  }

  @Override
  public void setActiveShape(HighlightShape as) {
    super.setActiveShape(as);
    if (as != null) myOptions.setColor(as.getColor());
  }

  protected boolean isMadeByLayer(AbstractShape shape) {
    return shape instanceof HighlightShape;
  }

  public Class getStorageLayerClass() {
    return MultipleShapeStorageLayer.class;
  }

  public StorageLayer createStorageLayer() {
    return new MultipleShapeStorageLayer(myWorkingImage);
  }
}
