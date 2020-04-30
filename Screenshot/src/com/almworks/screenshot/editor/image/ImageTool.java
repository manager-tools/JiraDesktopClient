package com.almworks.screenshot.editor.image;

import com.almworks.screenshot.editor.layers.ActionLayer;
import com.almworks.util.components.plaf.macosx.Aqua;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ImageTool {

  protected final WorkingImage myWorkingImage;

  protected final String myName;

  protected Icon myIcon;

  protected int myKey;

  private ToolButton myToolButton;

  public String getName() {
    return myName;
  }

  public int getKeyCode() {
    return myKey;
  }


  public ImageTool(WorkingImage workingImage, String name, Icon icon, int ks) {
    myWorkingImage = workingImage;
    myName = name;
    myIcon = icon;
    myKey = ks;
  }

  public void installWidgets(ImageToolbarBuilder box, ButtonGroup g) {
    ImageToolAction toolAction = new ImageToolAction(myName, myIcon, myWorkingImage, this);
    myToolButton = new ToolButton(toolAction);
    box.addTool(myToolButton);
    g.add(myToolButton);
  }

  abstract public boolean isOwner(ActionLayer layer);

  abstract public ActionLayer createActionLayer();

  public void activate() {
    myToolButton.setSelected(true);
  }

  public class ToolButton extends JToggleButton implements ActionListener {
    private ImageToolAction action;

    public ToolButton(ImageToolAction action) {
      super(action.getText(), action.getIcon());
      this.action = action;
      setMnemonic(myKey);
      setFocusable(false);
      addActionListener(this);
      setToolTipText(action.getText() + " (" + Character.toString((char) myKey) + ")");
      Aqua.makeSquareButton(this);
    }

    public void actionPerformed(ActionEvent e) {
      action.doPerform();
    }
  }

}
