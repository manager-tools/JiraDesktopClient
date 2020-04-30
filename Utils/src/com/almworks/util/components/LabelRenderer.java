package com.almworks.util.components;

import com.almworks.util.components.renderer.CellState;

import javax.swing.*;
import javax.swing.border.Border;

public abstract class LabelRenderer <T> implements CollectionRenderer<T> {
  protected final ALabel myLabel = new ALabel();
  private Border myInnerBorder;

  protected LabelRenderer() {
    myLabel.setOpaque(true);
  }

  public JComponent getRendererComponent(CellState state, T element) {
    setState(state);
    setElement(element, state);
    return myLabel;
  }

  protected abstract void setElement(T element, CellState state);

  private void setState(CellState state) {
    state.setToLabel(myLabel, myInnerBorder);
  }

  public void setInnerBorder(Border innerBorder) {
    myInnerBorder = innerBorder;
  }
}
