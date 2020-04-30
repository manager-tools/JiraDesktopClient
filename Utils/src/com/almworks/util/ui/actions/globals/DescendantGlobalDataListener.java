package com.almworks.util.ui.actions.globals;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

/**
 * @author dyoma
 */
public abstract class DescendantGlobalDataListener implements ContainerListener {
  public void componentAdded(ContainerEvent e) {
    Container container = getTargetContainer(e);
    if (container == null)
      return;
    if (DataGlobalizationUtil.isDataRoot(container)) {
      subTreeAdded(container);
      return;
    }
    java.util.List<Component> components = DataGlobalizationUtil.descendants(container);
    components.add(container);
    for (Component descendant : components) {
      if (!DataGlobalizationUtil.isDataRoot(descendant))
        addContainerListenerTo(descendant);
    }
    subTreeAdded(container);
  }

  public void componentRemoved(ContainerEvent e) {
    Container container = getTargetContainer(e);
    if (container == null)
      return;
    stopListen(container);
    subTreeRemoved(container);
  }

  @Nullable
  private Container getTargetContainer(ContainerEvent e) {
    Component child = e.getChild();
    return child instanceof Container && !(child instanceof CellRendererPane) ? (Container) child : null;
  }

  private void stopListen(Container container) {
    container.removeContainerListener(this);
    for (int i = 0; i < container.getComponentCount(); i++) {
      Component child = container.getComponent(i);
      if (child instanceof Container)
        stopListen((Container) child);
    }
  }

  private void addContainerListenerTo(Component descendant) {
    if (descendant instanceof Container)
      ((Container) descendant).addContainerListener(this);
  }

  protected abstract void subTreeAdded(Container container);

  protected abstract void subTreeRemoved(Container container);
}
