package com.almworks.util.components.plaf;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class DelegatingButtonUI extends ButtonUI {
  protected final ComponentUI myDelegate;

  public DelegatingButtonUI(JComponent target) {
    myDelegate = UIManager.getUI(target);
  }

  public boolean contains(JComponent c, int x, int y) {
    return myDelegate.contains(c, x, y);
  }

  public Accessible getAccessibleChild(JComponent c, int i) {
    return myDelegate.getAccessibleChild(c, i);
  }

  public int getAccessibleChildrenCount(JComponent c) {
    return myDelegate.getAccessibleChildrenCount(c);
  }

  public Dimension getMaximumSize(JComponent c) {
    return myDelegate.getMaximumSize(c);
  }

  public Dimension getMinimumSize(JComponent c) {
    return myDelegate.getMinimumSize(c);
  }

  public Dimension getPreferredSize(JComponent c) {
    return myDelegate.getPreferredSize(c);
  }

  public void installUI(JComponent c) {
    myDelegate.installUI(c);
  }

  public void paint(Graphics g, JComponent c) {
    myDelegate.paint(g, c);
  }

  public void uninstallUI(JComponent c) {
    myDelegate.uninstallUI(c);
  }

  public void update(Graphics g, JComponent c) {
    // do not delegate
    super.update(g, c);
  }
}
