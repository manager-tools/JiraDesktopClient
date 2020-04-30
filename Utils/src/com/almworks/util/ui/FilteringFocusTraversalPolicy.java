package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;

public class FilteringFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
  public static final ComponentProperty<Boolean> NO_FOCUS = ComponentProperty.createProperty("NO_FOCUS");

  protected boolean accept(Component c) {
    if (!super.accept(c))
      return false;
    if (c instanceof JComponent) {
      Boolean noFocus = NO_FOCUS.getClientValue((JComponent) c);
      if (noFocus != null && noFocus) {
        return false;
      }
    }
    return true;
  }
/*
  public Component getFirstComponent(Container aContainer) {
    return super.getFirstComponent(aContainer);
  }

  public Component getLastComponent(Container aContainer) {
    return super.getLastComponent(aContainer);
  }

  public Component getInitialComponent(Window window) {
    return super.getInitialComponent(window);
  }

  public Component getDefaultComponent(Container aContainer) {
    return super.getDefaultComponent(aContainer);
  }

  public Component getComponentAfter(Container aContainer, Component aComponent) {
    return super.getComponentAfter(aContainer, aComponent);
  }

  public Component getComponentBefore(Container aContainer, Component aComponent) {
    return super.getComponentBefore(aContainer, aComponent);
  }*/
}
