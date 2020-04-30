package com.almworks.util.components;

import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author : Dyoma
 */
public class PlaceHolder extends JPanel implements UIComponentWrapper, PropertyChangeListener {
  private final Lifecycle myComponentLife = new Lifecycle();
  private UIComponentWrapper myComponent;
  private boolean myComponentToDispose = false;
  private boolean myDisplayNotified = false;

  public PlaceHolder() {
    UIUtil.setupTransparentPanel(this);
  }

  public void showThenDispose(UIComponentWrapper component) {
    show(component);
    myComponentToDispose = true;
  }

  /**
   * @see com.almworks.util.components.tabs.ContentTab#setComponent(com.almworks.util.ui.UIComponentWrapper)
   */
  public void show(@Nullable UIComponentWrapper component) {
    if (myComponent == component)
      return;
    dispose();
    myComponent = component;
    if (myComponent != null) {
      final JComponent widget = myComponent.getComponent();
      if (widget != null) {
        add(widget);
        widget.addPropertyChangeListener("visible", this);
        myComponentLife.lifespan().add(new Detach() {
          protected void doDetach() throws Exception {
            widget.removePropertyChangeListener("visible", PlaceHolder.this);
          }
        });
      }
    }
    revalidate();
    repaint();
  }

  public UIComponentWrapper getShown() {
    return myComponent;
  }

  /**
   * @see com.almworks.util.components.tabs.ContentTab#setComponent(javax.swing.JComponent)
   */
  public void show(JComponent component) {
    if (myComponent != null && myComponent.getComponent() == component)
      return;
    show(component != null ? new Simple(component) : null);
  }

  public void clear() {
    show((JComponent) null);
  }

  public void addNotify() {
    super.addNotify();
    notifyDisplayable();
  }

  public void removeNotify() {
    myDisplayNotified = false;
    super.removeNotify();
  }

  private void notifyDisplayable() {
    if (myDisplayNotified)
      return;
    myDisplayNotified = true;
    if (myComponent instanceof UIComponentWrapper.DisplayableListener)
      ((DisplayableListener) myComponent).onComponentDisplayble();
  }

  public JComponent getComponent() {
    return this;
  }

  public void dispose() {
    myDisplayNotified = false;
    myComponentLife.cycle();
    if (myComponent != null) {
      removeAll();
      if (myComponentToDispose)
        myComponent.dispose();
      myComponent = null;
      myComponentToDispose = false;
    }
  }

  /**
   * @deprecated
   */
  @Override
  public void layout() {
    Component component = getDisplayedComponent();
    if (component != null) {
      Insets insets = getInsets();
      int width = Math.max(0, getWidth() - insets.left - insets.right);
      int height = Math.max(0, getHeight() - insets.top - insets.bottom);
      component.setBounds(insets.left, insets.top, width, height);
    }
    if (isDisplayable()) {
      notifyDisplayable();
    }
  }

  public Dimension getMaximumSize() {
    Component component = getDisplayedComponent();
    return (component != null && component.isVisible()) ? addInsets(component.getMaximumSize()) : new Dimension(0, 0);
  }

  public Dimension getMinimumSize() {
    Component component = getDisplayedComponent();
    return (component != null && component.isVisible()) ? addInsets(component.getMinimumSize()) : new Dimension(0, 0);
  }

  public Dimension getPreferredSize() {
    Component component = getDisplayedComponent();
    return (component != null && component.isVisible()) ? addInsets(component.getPreferredSize()) : new Dimension(0, 0);
  }

  private Dimension addInsets(Dimension dimension) {
    Insets insets = getInsets();
    return new Dimension(dimension.width + insets.left + insets.right, dimension.height + insets.top + insets.bottom);
  }

  @Nullable
  public Component getDisplayedComponent() {
    int componentCount = getComponentCount();
    assert componentCount < 2 : componentCount;
    if (componentCount == 0)
      return null;
    return getComponent(0);
  }

  public void propertyChange(PropertyChangeEvent evt) {
    revalidate();
    repaint();
  }
}
