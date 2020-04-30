package com.almworks.util.ui.actions.dnd;

import javax.swing.*;

public interface DndTarget {
  JComponent getTargetComponent();

  void dragNotify(DndEvent event);
}
