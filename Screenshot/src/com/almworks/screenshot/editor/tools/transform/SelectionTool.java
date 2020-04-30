package com.almworks.screenshot.editor.tools.transform;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

/**
 * @author Stalex
 */

public class SelectionTool extends ImageTool {
  public SelectionTool(WorkingImage workingImage) {
    super(workingImage, "Select", null, KeyEvent.VK_S);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof SelectionActionLayer;
  }

  public ActionLayer createActionLayer() {
    return new SelectionActionLayer(myWorkingImage);
  }
}

