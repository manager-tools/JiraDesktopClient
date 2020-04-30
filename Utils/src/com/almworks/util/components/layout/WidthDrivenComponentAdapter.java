package com.almworks.util.components.layout;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class WidthDrivenComponentAdapter implements WidthDrivenComponent {
  @NotNull
  private final JComponent myComponent;

  public WidthDrivenComponentAdapter(JComponent component) {
    assert component != null;
    myComponent = component;
  }

  public int getPreferredWidth() {
    return myComponent.getPreferredSize().width;
  }

  public int getPreferredHeight(int width) {
    return myComponent.getPreferredSize().height;
  }

  @NotNull
  public JComponent getComponent() {
    return myComponent;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }
}
