package com.almworks.util.components.plaf.macosx.combobox;

import javax.swing.*;
import java.awt.*;

public class MacPrettyComboBox extends JComboBox {
  private static final boolean UNAVAILABLE = !MacComboBoxEditor.IS_AVAILABLE;
  private static final int NOT_PAINTING = 0;
  private static final int CHILDREN = 1;
  private static final int BUTTON = 2;
  
  private int myPaintStage = NOT_PAINTING;
  
  public MacPrettyComboBox() {
    super();
    if (MacComboBoxEditor.IS_AVAILABLE) {
      MacComboBoxEditor.install(this);
    }
  }

  @Override
  public void paint(Graphics g) {
    if (UNAVAILABLE) {
      super.paint(g);
      return;
    }
    super.paint(g);
    MacComboBoxFocusRing.paintComboboxFocusRing(this, g, getFocusRingInsets());
  }

  protected Insets getFocusRingInsets() {
    return null;
  }

  @Override
  protected void paintChildren(Graphics g) {
    if (UNAVAILABLE) {
      super.paintChildren(g);
      return;
    }
    myPaintStage = CHILDREN;
    try {
      super.paintChildren(g);
    } finally {
      myPaintStage = NOT_PAINTING;
    }
  }

  @Override
  public Component getComponent(int n) {
    if (UNAVAILABLE || myPaintStage == NOT_PAINTING) {
      return super.getComponent(n);
    }
    Component child = super.getComponent(n);
    if (child instanceof JButton) {
      myPaintStage = BUTTON;
    } else {
      myPaintStage = CHILDREN;
    }
    return child;
  }
  
  public boolean isPaintingButton() {
    return myPaintStage == BUTTON;
  }
}
