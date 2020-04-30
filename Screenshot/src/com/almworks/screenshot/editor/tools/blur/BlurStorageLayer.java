package com.almworks.screenshot.editor.tools.blur;

import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.MultipleShapeStorageLayer;

import java.awt.*;

public class BlurStorageLayer extends MultipleShapeStorageLayer {

  private static final Color MY_SIMPLE_BLUR_COLOR = new Color(1.0F, 1.0F, 1.0F, 0.25F);

  protected final WorkingImage myWorkingImage;

//  protected LinkedList<Rectangle> Rectangles = new LinkedList<Rectangle>();


  public BlurStorageLayer(WorkingImage workingImage) {
    super(workingImage);
    myWorkingImage = workingImage;
  }
}
