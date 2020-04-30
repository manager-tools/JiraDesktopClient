package com.almworks.util.ui;

import java.awt.event.*;

public abstract class MegaMouseAdapter implements MouseListener, MouseMotionListener, MouseWheelListener {
  public void mouseClicked(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseEntered(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseExited(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mousePressed(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseReleased(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseDragged(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseMoved(MouseEvent e) {
    onMouseEvent(e);
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
  }

  protected void onMouseEvent(MouseEvent e) {
  }
}
