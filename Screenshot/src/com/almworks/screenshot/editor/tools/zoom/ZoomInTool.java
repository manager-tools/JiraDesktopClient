package com.almworks.screenshot.editor.tools.zoom;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

/**
 * @author
 */
public class ZoomInTool extends ImageTool {
  public ZoomInTool(WorkingImage workingImage) {
    super(workingImage, "Magnify", null, KeyEvent.VK_M);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof ZoomInActionLayer;
  }

  public ActionLayer createActionLayer() {
    return new ZoomInActionLayer(myWorkingImage);
  }
}
