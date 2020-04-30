package com.almworks.util.ui.actions.presentation;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author dyoma
 */
public abstract class BasePopupHandler extends MouseAdapter {
  public void mousePressed(MouseEvent e) {
    handleMouseEvent(e);
  }

  public void mouseReleased(MouseEvent e) {
    handleMouseEvent(e);
  }

  public void mouseClicked(MouseEvent e) {
    handleMouseEvent(e);
  }

  private void handleMouseEvent(final MouseEvent e) {
    if (!e.isPopupTrigger())
      return;
    Object source = e.getSource();
    assert source instanceof Component;
    if (!((Component) source).isShowing())
      return;
    showPopupMenu(e, (Component) source);
  }

  protected abstract void showPopupMenu(MouseEvent e, Component source);
}
