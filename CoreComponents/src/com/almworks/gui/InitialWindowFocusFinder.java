package com.almworks.gui;

import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

public class InitialWindowFocusFinder {
  private static final ComponentProperty<Boolean> INITIAL_WINDOW_FOCUS_OWNER = ComponentProperty.createProperty("INITIAL_WINDOW_FOCUS_OWNER");

  public static void setInitialWindowComponent(JComponent c) {
    INITIAL_WINDOW_FOCUS_OWNER.putClientValue(c, Boolean.TRUE);
  }

  /**
   * performs wide-scan of the component tree
   */
  public static Component findInitialWindowComponent(Container window) {
    LinkedList<Component> todo = new LinkedList<Component>();
    todo.add(window);
    while (!todo.isEmpty()) {
      Component c = todo.removeFirst();
      if (c instanceof JComponent) {
        Boolean value = INITIAL_WINDOW_FOCUS_OWNER.getClientValue((JComponent) c);
        if (value == Boolean.TRUE) {
          return c;
        }
      }
      if (c instanceof Container) {
        Container container = (Container) c;
        int count = container.getComponentCount();
        for (int i = 0; i < count; i++) {
          todo.add(container.getComponent(i));
        }
      }
    }
    return null;
  }

  public static void focusInitialComponent(Container parent) {
    Component c = findInitialWindowComponent(parent);
    if (c != null) {
      UIUtil.requestFocusInWindowLater(c);
    }
  }
}
