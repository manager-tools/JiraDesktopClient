package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;

public abstract class ComponentVisitor {
  public void processComponent(Component c) {
    if (c instanceof JLabel) {
      visitJLabel((JLabel)c);
    } else if (c instanceof JComponent) {
      visitJComponent((JComponent) c);
    } else if (c instanceof Container) {
      visitContainer((Container) c);
    } else {
      visitComponent(c);
    }
  }

  protected void visitJLabel(JLabel c) {
    visitJComponent(c);
  }

  protected void visitJComponent(JComponent c) {
    visitContainer(c);
  }

  protected void visitContainer(Container c) {
    visitComponent(c);
  }

  protected void visitComponent(Component c) {
  }
}
