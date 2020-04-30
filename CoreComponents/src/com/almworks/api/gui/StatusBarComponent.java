package com.almworks.api.gui;

import com.almworks.util.ui.UIComponentWrapper;

import javax.swing.*;

public interface StatusBarComponent extends UIComponentWrapper {
  int getReservedWidth();

  /**
   * Called when component is added to the status bar.
   */
  void attach();

  class Simple implements StatusBarComponent {
    private final JComponent myComponent;

    public Simple(JComponent component) {
      assert component != null;
      myComponent = component;
    }

    public void attach() {
    }

    public int getReservedWidth() {
      return 0;
    }

    public void dispose() {
    }

    public JComponent getComponent() {
      return myComponent;
    }
  }
}
