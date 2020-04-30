package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

// todo unit test

public class NeighbourAwareSeparator extends JPopupMenu.Separator {
  public void addNotify() {
    super.addNotify();
    Container container = getParent();
    assert container != null;
    container.addContainerListener(new ContainerAdapter() {
      public void componentAdded(ContainerEvent e) {
        checkVisibility();
      }

      public void componentRemoved(ContainerEvent e) {
        if (e.getComponent() == NeighbourAwareSeparator.this)
          e.getContainer().removeContainerListener(this);
        checkVisibility();
      }
    });
    checkVisibility();
  }

  public void checkVisibility() {
    Container parent = getParent();
    if (!(parent instanceof JPopupMenu))
      return;
    int i = ((JPopupMenu) parent).getComponentIndex(this);
    Component[] components = parent.getComponents();
    int count = components.length;
    final boolean visible;
    if (i <= 0 || i >= count - 1) {
      visible = false;
    } else {
      // A little trick to make sure there are no adjacent separators:
      // When searching visible item up the menu, stop when separator is found. When searching down, don't stop.
      //
      visible = findVisible(components, i - 1, 0, true) && findVisible(components, i + 1, count - 1, false);
    }
    setVisible(visible);
  }

  /**
   * Goes through components to find a visible one. If stopOnSeparator is true, doesn't go behind separator
   * @return true if there's a visible component
   */
  private boolean findVisible(Component[] components, int from, int to, boolean stopOnSeparator) {
    int step = to >= from ? 1 : -1;
    for (int i = from; (to - i) * step >= 0; i += step) {
      assert i >= 0 && i < components.length;
      Component component = components[i];
      if (component instanceof JSeparator) {
        if (stopOnSeparator)
          return false;
        else
          continue;
      }
      if (component.isVisible())
        return true;
    }
    return false;
  }
}
