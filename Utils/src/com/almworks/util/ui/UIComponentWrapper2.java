package com.almworks.util.ui;

import org.almworks.util.detach.Detach;

import javax.swing.*;

public interface UIComponentWrapper2 extends UIComponentWrapper {
  @Deprecated
  void dispose();

  Detach getDetach();

  class SimpleDetachable implements UIComponentWrapper2 {
    private final JComponent myComponent;
    private final Detach myDetach;

    public SimpleDetachable(Detach detach, JComponent component) {
      myDetach = detach;
      myComponent = component;
    }

    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    @Override
    public Detach getDetach() {
      return myDetach;
    }

    @Override
    public void dispose() {
      myDetach.detach();
    }
  }
}
