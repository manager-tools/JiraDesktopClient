package com.almworks.screenshot.editor.tools.annotate;

import com.almworks.screenshot.editor.image.ImageTool;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.ActionLayer;

import java.awt.event.KeyEvent;

public class AnnotateTool extends ImageTool {
  public AnnotateTool(WorkingImage workingImage) {
    super(workingImage, "Comment", null, KeyEvent.VK_O);
  }

  public boolean isOwner(ActionLayer layer) {
    return layer instanceof AnnotationLayer;
  }

  public ActionLayer createActionLayer() {
    return new AnnotationLayer(myWorkingImage);
  }
}