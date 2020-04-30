package com.almworks.util.ui;

import com.almworks.util.LogHelper;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.ui.actions.presentation.BasePopupHandler;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

public abstract class PopupMenuListener extends BasePopupHandler implements KeyListener {
  protected abstract void showPopup(JComponent component, int x, int y, InputEvent event);

  public void attach(Lifespan life, JComponent component) {
    UIUtil.addMouseListener(life, component, this);
    UIUtil.addKeyListener(life, component, this);
  }

  @Override
  protected void showPopupMenu(MouseEvent e, Component source) {
    Component component = e.getComponent();
    assert component instanceof JComponent : e.getSource() + " " + e;
    int x = e.getX();
    int y = e.getY();
    showPopup((JComponent) component, x, y, e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == 0x020D) {//KeyEvent.VK_CONTEXT_MENU
      Component source = e.getComponent();
      assert source instanceof JComponent : source;
      JComponent component = (JComponent) source;
      ACollectionComponent<?> aCollection = SwingTreeUtil.findAncestorOfType(component, ACollectionComponent.class);
      int x = 0;
      int y = 0;
      boolean fixViewport = true;
      if (aCollection != null) {
        Rectangle rect = aCollection.getElementRect(aCollection.getSelectionAccessor().getSelectedIndex());
        if (rect != null) {
          x = rect.x;
          y = rect.y + rect.height + 1;
        }
      } else {
        JTextComponent textComponent = SwingTreeUtil.findAncestorOfType(component, JTextComponent.class);
        if (textComponent != null) {
          int position = textComponent.getCaretPosition();
          try {
            Rectangle rectangle = textComponent.getUI().modelToView(textComponent, position);
            x = rectangle.x + rectangle.width / 2;
            y = rectangle.y + rectangle.height / 2;
            fixViewport = false;
          } catch (BadLocationException e1) {
            LogHelper.warning(e1);
          }
        }
      }
      if (fixViewport) {
        JViewport viewport = SwingTreeUtil.findAncestorOfType(component, JViewport.class);
        if (viewport != null) {
          Rectangle pos = viewport.getViewRect();
          x = Math.max(x, pos.x);
          x = Math.min(x, pos.x + pos.width);
          y = Math.max(y, pos.y);
          y = Math.min(y, pos.y + pos.height);
        }
      }
      showPopup(component, x, y, e);
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void keyPressed(KeyEvent e) {}
}
