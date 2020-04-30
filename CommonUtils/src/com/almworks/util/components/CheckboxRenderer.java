package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

class CheckboxRenderer extends BackgroundCanvasRenderer {
  private static final int GAP = 5;

  private final AbstractButton myCheckBox;
  private final ButtonModel myCheckBoxModel;

  private Dimension myCheckBoxPreferredSize;

  public static CheckboxRenderer newCheckBoxRenderer() {
    final JCheckBox button = new JCheckBox();
    button.setBorderPaintedFlat(true);
    return new CheckboxRenderer(button);
  }

  public static CheckboxRenderer newRadioButtonRenderer() {
    return new CheckboxRenderer(new JRadioButton());
  }

  private CheckboxRenderer(AbstractButton button) {
    myCheckBox = button;
    myCheckBoxModel = myCheckBox.getModel();
    add(myCheckBox);
    myCheckBox.setOpaque(false);
    if(Env.isMac()) {
      myCheckBox.putClientProperty("JComponent.sizeVariant", "small");
    }
  }

  @Override
  protected void layoutRenderer() {
    setBoundsBackground(0, 0, getWidth(), getHeight());
    Rectangle client = TEMP_RECTANGLE.getInstance();
    client.setBounds(0, 0, getWidth(), getHeight());
    getClientCellRect(client);
    int top = client.y;
    int clientWidth = client.width;
    int clientHeight = client.height;
    getCheckboxLocation(client);
    int canvasX = client.x + client.width + GAP;
    AwtUtil.setBounds(myCheckBox, client);
    TEMP_RECTANGLE.releaseInstance(client);
    setBoundsCanvas(canvasX, top, clientWidth - canvasX, clientHeight);
  }

  public void getClientCellRect(Rectangle cellBounds) {
    Insets bgInsets = getBackgroundInsets();
    cellBounds.x += bgInsets.left;
    cellBounds.y += bgInsets.top;
    cellBounds.width -= AwtUtil.getInsetWidth(bgInsets);
    cellBounds.height -= AwtUtil.getInsetHeight(bgInsets);
  }

  public void getCheckboxLocation(Rectangle clientRect) {
    Dimension cbSize = getCheckBoxPrefSize();
    int height = clientRect.height;
    clientRect.y += (height - cbSize.height) / 2;
    clientRect.width = cbSize.width;
    clientRect.height = cbSize.height;
  }

  public Dimension getPreferredSize() {
    Dimension dimension = super.getPreferredSize();
    Dimension size = getCheckBoxPrefSize();
    size.width += GAP + dimension.width + GAP; // The second GAP is just aesthetics
    size.height = dimension.height;
    return size;
  }

  // PLO-507 Opening Assignee constraint editor in Query Builder is slow
  // profiling has shown that JCheckBox.getPreferredSize() was called too often, which caused slowdown
  private Dimension getCheckBoxPrefSize() {
    if (myCheckBoxPreferredSize == null) {
      myCheckBoxPreferredSize = myCheckBox.getPreferredSize();
    }
    return new Dimension(myCheckBoxPreferredSize);
  }

  public void paint(Graphics g) {
    super.paint(g);
    Rectangle rect = TEMP_RECTANGLE.getInstance();
    try {
      paintChild(g, myCheckBox, rect);
    } finally{
      TEMP_RECTANGLE.releaseInstance(rect);
    }
  }

  public void setChecked(boolean selected) {
    if (selected != myCheckBoxModel.isSelected())
      myCheckBoxModel.setSelected(selected);
    if (myCheckBoxModel.isArmed())
      myCheckBoxModel.setArmed(false);
    if (myCheckBoxModel.isPressed())
      myCheckBoxModel.setPressed(false);
    if (myCheckBoxModel.isRollover())
      myCheckBoxModel.setRollover(false);
  }

  @Override
  public void setEnabled(boolean enabled) {
    myCheckBox.setEnabled(enabled);
  }
}
