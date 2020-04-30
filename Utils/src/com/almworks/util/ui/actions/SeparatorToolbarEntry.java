package com.almworks.util.ui.actions;

import com.almworks.util.components.AToolbar;

import javax.swing.*;
import java.awt.*;

public class SeparatorToolbarEntry implements ToolbarEntry {
  public static final SeparatorToolbarEntry INSTANCE = new SeparatorToolbarEntry();

  private SeparatorToolbarEntry() {}

  public void addToToolbar(AToolbar toolbar) {
    toolbar.addSeparator();
  }

  public void addToPanel(JPanel panel, JComponent contextComponent) {
    JLabel label = new JLabel("");
    label.setPreferredSize(new Dimension(AToolbar.MINIMUM_SEPARATOR_WIDTH, 1));
    panel.add(label);
  }
}
