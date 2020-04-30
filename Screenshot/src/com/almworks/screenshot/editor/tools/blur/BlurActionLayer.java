package com.almworks.screenshot.editor.tools.blur;

import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.BaseLayerOptions;
import com.almworks.screenshot.editor.layers.SingleShapeActionLayer;
import com.almworks.screenshot.editor.layers.StorageLayer;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import javax.swing.event.ChangeEvent;
import java.awt.*;

public class BlurActionLayer extends SingleShapeActionLayer<BluredRegion, BlurActionLayer.BlurOptions> {
  public BlurActionLayer(WorkingImage workingImage) {
    super(workingImage);
    myOptions = new BlurOptions();
  }

  @Override
  public void setActiveShape(BluredRegion as) {
    super.setActiveShape(as);
    if (myActiveShape != null) {
      myOptions.setValue(as.getWeight());
    }
  }

  public BluredRegion createShape(Rectangle shape) {
    return new BluredRegion(shape, myWorkingImage, myOptions.getValue());
  }

  protected boolean isMadeByLayer(AbstractShape shape) {
    return shape instanceof BluredRegion;
  }

  public Class getStorageLayerClass() {
    return BlurStorageLayer.class;
  }

  public StorageLayer createStorageLayer() {
    return new BlurStorageLayer(myWorkingImage);
  }

  class BlurOptions extends BaseLayerOptions.SliderOptions {
    public static final int MAX_VAL = 10;

    public BlurOptions() {
      super("Blur", "Click and drag to blur an area.");
      setSliderParams(1, MAX_VAL, 3);
    }

    public void stateChanged(ChangeEvent e) {
      super.stateChanged(e);
      if (myActiveShape != null) {
        myActiveShape.setWeight(getValue());
        myWorkingImage.reportDirty(myActiveShape.getBounds());
      }
    }
  }
}
