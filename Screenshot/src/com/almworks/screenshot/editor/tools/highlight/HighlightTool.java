package com.almworks.screenshot.editor.tools.highlight;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

/**
 * @author Stalex
 */
public class HighlightTool extends ImageTool {
  public HighlightTool(WorkingImage workingImage) {
    super(workingImage, "Emphasize", null, KeyEvent.VK_E);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof HighlightActionLayer;
  }

  public ActionLayer createActionLayer() {
    myWorkingImage.requestFocus();
    return new HighlightActionLayer(myWorkingImage);
  }
}
