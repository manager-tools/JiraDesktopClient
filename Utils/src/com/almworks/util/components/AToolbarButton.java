package com.almworks.util.components;

import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;

public class AToolbarButton extends AActionButton {
  private static final String uiClassID = "AToolbarButtonUI";
  private static final int CLICK_THRESHOLD = 500;
  private Color myHighlight;

  public AToolbarButton(AnAction action) {
    super(action);
    init();
  }

  public AToolbarButton() {
    super();
    init();                                                  
  }

  public static AToolbarButton withAction(Action action) {
    AToolbarButton button = new AToolbarButton();
    button.setAction(action);
    return button;
  }

  public Color getHighlight() {
    return myHighlight;
  }

  public String getUIClassID() {
    return uiClassID;
  }

  public void setHighlight(Color color) {
    Color old = getHighlight();
    myHighlight = color;
    firePropertyChange("highlight", old, color);
    if (!Util.equals(old, color)) {
      repaint();
    }
  }

  public void updateUI() {
    setUI((ButtonUI) UIManager.getUI(this));
  }

  private void init() {
    setContentAreaFilled(false);
    setMultiClickThreshhold(CLICK_THRESHOLD);
    PresentationMapping.clearMnemonic(this);
  }

  public Dimension getPreferredSize() {
    Dimension result = super.getPreferredSize();
    result.width = Math.max(result.width, 16);
    result.height = Math.max(result.height, 16);
    return result;
  }
}
