package com.almworks.screenshot.editor.image;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public abstract class KeyEventAdapter implements KeyListener {
  public void keyPressed(KeyEvent e) {
    onKeyEvent(e);
  }

  public void keyReleased(KeyEvent e) {
    onKeyEvent(e);
  }

  public void keyTyped(KeyEvent e) {
    onKeyEvent(e);
  }

  protected void onKeyEvent(KeyEvent e) {
  }
}
