package com.almworks.screenshot.editor.tools.blur;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

public class BlurTool extends ImageTool {

  public BlurTool(WorkingImage workingImage) {
    super(workingImage, "Blur", null, KeyEvent.VK_B);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof BlurActionLayer;
  }

  public ActionLayer createActionLayer() {
    myWorkingImage.requestFocus();
    return new BlurActionLayer(myWorkingImage);
  }
}
