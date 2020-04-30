package com.almworks.screenshot.editor.image;

import com.almworks.screenshot.editor.layers.ActionLayer;

import javax.swing.*;

public class ImageToolAction {
  private final ImageTool myTool;
  private final WorkingImage myImage;
  private Icon myIcon;
  private String myText;

  public ImageToolAction(String text, Icon icon, WorkingImage image, ImageTool tool) {
    assert tool != null;
    assert image != null;
    myImage = image;
    myTool = tool;
    myIcon = icon;
    myText = text;
  }

  public String getText() {
    return myText;
  }

  public Icon getIcon() {
    return myIcon;
  }

  protected void doPerform() {
    if (!isActive()) {
      myImage.changeActionLayer(myTool.createActionLayer());
      myImage.requestFocus();
    }
  }

  private boolean isActive() {
    ActionLayer actionLayer = myImage.getActionLayer();
    return actionLayer != null && myTool.isOwner(actionLayer);
  }
}
