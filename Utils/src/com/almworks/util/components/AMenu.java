package com.almworks.util.components;

import com.almworks.util.ui.actions.ActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;

/**
 * @author dyoma
 */
public class AMenu extends JMenu implements AMenuChild {
  private final ComponentAdapter myChildVisibilityListener = new ComponentAdapter() {
    public void componentShown(ComponentEvent e) {
      updateActive(e.getComponent());
    }

    public void componentHidden(ComponentEvent e) {
      updateActive(e.getComponent());
    }
  };
  @NotNull
  private final UpdatePolicy myPolicy;

  public AMenu(@NotNull  UpdatePolicy policy) {
    myPolicy = policy;
    myPolicy.setActive(false, this);
  }

  public void updateItems() {
    ActionUtil.updateMenu(this);
  }

  public JMenuItem add(JMenuItem menuItem) {
    JMenuItem result = super.add(menuItem);
    addChild(menuItem);
    return result;
  }

  public void remove(JMenuItem item) {
    removeChild(item);
    super.remove(item);
  }

  public void parentStartsUpdate() {
    for (Component component1 : getMenuComponents())
      if (component1 instanceof AMenuChild)
        ((AMenuChild) component1).parentStartsUpdate();
  }

  public void parentStopsUpdate() {
    for (Component component1 : getMenuComponents())
      if ((component1 instanceof AMenuChild))
        ((AMenuChild) component1).parentStopsUpdate();
  }

  public static AMenu hidding() {
    return new AMenu(UpdatePolicy.VISIBLE);
  }

  public static AMenu disabling() {
    return new AMenu(UpdatePolicy.ENABLE);
  }

  public interface UpdatePolicy {
    void setActive(boolean active, @NotNull Component component);

    boolean isActive(@NotNull Component component);

    UpdatePolicy ENABLE = new UpdatePolicy() {
      public void setActive(boolean active, @NotNull Component component) {
        component.setEnabled(active);
      }

      public boolean isActive(@NotNull Component component) {
        return component.isEnabled();
      }

      public String toString() {
        return "ENABLE";
      }
    };

    UpdatePolicy VISIBLE = new UpdatePolicy() {
      public void setActive(boolean active, @NotNull  Component component) {
        component.setVisible(active);
      }

      public boolean isActive(@NotNull Component component) {
        return component.isVisible();
      }

      public String toString() {
        return "VISIBLE";
      }
    };
  }

  private void updateActive(Component changed) {
    assert Arrays.asList(getPopupMenu().getComponents()).contains(changed) : changed;
    if (changed.isVisible()) {
      myPolicy.setActive(true, this);
    } else {
      checkShouldDeactivate();
    }
  }

  private void checkShouldDeactivate() {
    if (!myPolicy.isActive(this))
      return;
    for (Component component : this.getMenuComponents()) {
      if (component.isVisible())
        return;
    }
    myPolicy.setActive(false, this);
  }

  private void addChild(JMenuItem menuItem) {
    menuItem.addComponentListener(myChildVisibilityListener);
    updateActive(menuItem);
  }

  private void removeChild(JMenuItem item) {
    item.removeComponentListener(myChildVisibilityListener);
    checkShouldDeactivate();
    if (item instanceof AMenuChild)
      ((AMenuChild) item).parentStopsUpdate();
  }

}
