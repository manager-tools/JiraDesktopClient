package com.almworks.util.ui.actions;

import com.almworks.util.components.AToolbar;

import javax.swing.*;

public interface ToolbarEntry {
  void addToToolbar(AToolbar toolbar);

  void addToPanel(JPanel panel, JComponent contextComponent);
}
