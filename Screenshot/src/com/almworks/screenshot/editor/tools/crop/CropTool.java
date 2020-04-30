package com.almworks.screenshot.editor.tools.crop;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

public class CropTool extends ImageTool {
  public CropTool(WorkingImage workingImage) {
    super(workingImage, "Crop", null, KeyEvent.VK_C);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof CropLayer;
  }

  public ActionLayer createActionLayer() {
    myWorkingImage.requestFocus();
    return new CropLayer(myWorkingImage);
  }
}
