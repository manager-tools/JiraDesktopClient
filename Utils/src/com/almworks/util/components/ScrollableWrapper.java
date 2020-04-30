package com.almworks.util.components;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import com.almworks.util.ui.actions.dnd.TransferHandlerBridge;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
class ScrollableWrapper<C extends JComponent & Scrollable> extends JComponent implements Scrollable {
  private final Object myScrollable;

  public ScrollableWrapper(C component) {
    assert component != null;
    myScrollable = component;
    add(getPrivateComponent());
    ActionContext.ACTUAL_COMPONENT.putClientValue(this, component);
  }

  /**
   * @deprecated
   */
  public void layout() {
    getPrivateComponent().setBounds(0, 0, getWidth(), getHeight());
  }

  public Dimension getPreferredSize() {
    return getPrivateComponent().getPreferredSize();
  }

  public Dimension getMaximumSize() {
    return getPrivateComponent().getMaximumSize();
  }

  public Dimension getMinimumSize() {
    return getPrivateComponent().getMinimumSize();
  }

  public void setPreferredSize(Dimension preferredSize) {
    getPrivateComponent().setPreferredSize(preferredSize);
  }

  public void setMinimumSize(Dimension minimumSize) {
    getPrivateComponent().setMinimumSize(minimumSize);
  }

  private Scrollable getPrivateScrollable() {
    // degenerator BUG
    return (Scrollable) myScrollable;
  }

  private JComponent getPrivateComponent() {
    // degenerator BUG
    return (JComponent) myScrollable;
  }

  public void setMaximumSize(Dimension maximumSize) {
    getPrivateComponent().setMaximumSize(maximumSize);
  }

  /**
   * @deprecated {@link #setTransfer(com.almworks.util.ui.actions.dnd.ContextTransfer)}
   */
  public void setTransferHandler(TransferHandler newHandler) {
    assert false;
  }

  public void setTransfer(ContextTransfer transfer) {
    JComponent component = getPrivateComponent();
    TransferHandlerBridge.install(component, transfer);
  }

  @Nullable
  public ContextTransfer getTransfer() {
    TransferHandler handler = getPrivateComponent().getTransferHandler();
    if (!(handler instanceof TransferHandlerBridge))
      return null;
    return ((TransferHandlerBridge) handler).getTransfer();
  }

  public boolean getScrollableTracksViewportHeight() {
    if (getPrivateScrollable().getScrollableTracksViewportHeight())
      return true;
    if (getParent() instanceof JViewport) {
      JViewport viewport = (JViewport) getParent();
      if (viewport.getParent() instanceof JScrollPane) {
        if (((JScrollPane) viewport.getParent()).getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER) {
          return true;
        }
      }
      return viewport.getHeight() > getPreferredSize().height;
    }
    return true;
  }

  public boolean getScrollableTracksViewportWidth() {
    if (getPrivateScrollable().getScrollableTracksViewportWidth())
      return true;
    if (getParent() instanceof JViewport) {
      JViewport viewport = (JViewport) getParent();
      if (viewport.getParent() instanceof JScrollPane) {
        if (((JScrollPane) viewport.getParent()).getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
          return true;
        }
      }
      return viewport.getWidth() > getPreferredSize().width;
    }
    return true;
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPrivateScrollable().getPreferredScrollableViewportSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getPrivateScrollable().getScrollableBlockIncrement(visibleRect, orientation, direction);
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getPrivateScrollable().getScrollableUnitIncrement(visibleRect, orientation, direction);
  }

  protected C getScrollable() {
    return (C) myScrollable;
  }

  public JComponent getSwingComponent() {
    return getPrivateComponent();
  }

  public boolean requestFocusInWindow() {
    return getPrivateComponent().requestFocusInWindow();
  }

  public void requestFocus() {
    getPrivateComponent().requestFocus();
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    getPrivateComponent().setEnabled(false);
  }
}
