package com.almworks.util.components.plaf.linux;

import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.*;

/*
Copyright (c) 2004, Brian Duff and contributors to winlaf.org
All rights reserved.

The contents of this file are subject to the BSD-style license distributed
with the source code in the file LICENSE. A copy of this license is available
at http://www.winlaf.org.
*/


/**
 * Utility class shared by multiple look and feel delegates which install
 * a context menu into text components.
 *
 * @author Brian.Duff@oracle.com
 * @author sereda (copied from winlaf, adjusted for linux)
 */
public final class TextComponentMenuCopied {
  private static final Object NO_TEXTFIELD_POPUP = "noTextFieldPopup";  //$NON-NLS-1$

  private final JTextComponent m_component;
  private Listener m_listener;
  private ActionMap m_actionMap;
  private UndoableEdit m_lastUndoableEdit;

  private static final String ACTION_UNDO = "undo";
  private static final String ACTION_CUT = "cut";
  private static final String ACTION_COPY = "copy";
  private static final String ACTION_PASTE = "paste";
  private static final String ACTION_DELETE = "delete";
  private static final String ACTION_SELECT_ALL = "selectAll";

  public TextComponentMenuCopied(JTextComponent component) {
    m_component = component;
    m_component.addMouseListener(m_listener = new Listener());
    m_component.addKeyListener(m_listener);
    m_component.getDocument().addUndoableEditListener(m_listener);
  }

  public void uninstall() {
    m_component.removeMouseListener(m_listener);
    m_component.removeKeyListener(m_listener);
    m_component.getDocument().removeUndoableEditListener(m_listener);
  }

  public void documentChanged(Document oldDocument, Document newDocument) {
    m_lastUndoableEdit = null;
    oldDocument.removeUndoableEditListener(m_listener);
    newDocument.addUndoableEditListener(m_listener);
  }

  private class Listener extends MouseAdapter implements KeyListener, UndoableEditListener {
    public void mousePressed(MouseEvent e) {
      mouseReleased(e);
    }

    public void mouseReleased(final MouseEvent me) {
      if (me.isPopupTrigger()) {
        if (me.getSource() instanceof JTextComponent) {
          final JTextComponent component = (JTextComponent) me.getSource();

          if (component.isEnabled()) {
            if (component.hasFocus()) {
              showPopupMenu(component, me);
            } else {
              component.requestFocusInWindow();
              // Must delay this because focus requests are asynchronous.
              EventQueue.invokeLater(new Runnable() {
                public void run() {
                  if (component.isDisplayable()) {
                    moveCaret(component, me);
                    showPopupMenu(component, me);
                  }
                }
              });
            }
          }
        }
      }
    }

    public void keyTyped(KeyEvent ke) {
    }

    public void keyPressed(KeyEvent ke) {
      // Shift-F10 is the popup trigger on Windows. It'd be nice to also
      // handle the windows popup menu key, but I've no idea how (keyCode == 0)
      // Also worth mentioning: Swing's keyboard handling of popup menus is
      // genuinely dire. You can't really keyboard navigate the popup even
      // when you do manage to invoke it using the keyboard. We have a hooge
      // wodge of code in Oracle that works around this, but I can't put it
      // here because it's not open source :(
      if ((ke.isShiftDown()) && ke.getKeyCode() == KeyEvent.VK_F10) {
        // Find the middle point of the field.
        int x = m_component.getWidth() / 2;
        int y = m_component.getHeight() / 2;
        showPopupMenu(m_component, x, y);
      }
    }

    public void keyReleased(KeyEvent ke) {
    }

    public void undoableEditHappened(UndoableEditEvent e) {
      if (e.getEdit().canUndo()) {
        m_lastUndoableEdit = e.getEdit();
      }
    }
  }

  private void createActionMap() {
    ActionMap map = new ActionMap();

    UndoAction undo = new UndoAction();
    resAction(undo, "&Undo");
    map.put(ACTION_UNDO, undo);
    map.put(ACTION_CUT, createDelegateAction(m_component.getActionMap().get(DefaultEditorKit.cutAction), "Cu&t"));
    map.put(ACTION_COPY, createDelegateAction(m_component.getActionMap().get(DefaultEditorKit.copyAction), "&Copy"));
    map.put(ACTION_PASTE, createDelegateAction(m_component.getActionMap().get(DefaultEditorKit.pasteAction), "&Paste"));
    map.put(ACTION_DELETE,
      createDelegateAction(m_component.getActionMap().get(DefaultEditorKit.deleteNextCharAction), "&Delete"));
    map.put(ACTION_SELECT_ALL,
      createDelegateAction(m_component.getActionMap().get(DefaultEditorKit.selectAllAction), "Select &All"));

    m_actionMap = map;
  }

  private ActionMap getActionMap() {
    if (m_actionMap == null) {
      createActionMap();
    }
    return m_actionMap;
  }

  private void showPopupMenu(JTextComponent component, int x, int y) {
    Boolean showPopup = (Boolean) component.getClientProperty(NO_TEXTFIELD_POPUP);
    if (showPopup == null || !showPopup.booleanValue()) {
      JPopupMenu popup = UIUtil.createJPopupMenu();

      updateActionEnablement();

      // TODO: Undo popup.add();
      // popup.addSeparator();
      ActionMap map = getActionMap();
      popup.add(map.get(ACTION_UNDO));
      popup.addSeparator();
      popup.add(map.get(ACTION_CUT));
      popup.add(map.get(ACTION_COPY));
      popup.add(map.get(ACTION_PASTE));
      popup.add(map.get(ACTION_DELETE));
      // TODO: Delete action.
      popup.addSeparator();
      popup.add(map.get(ACTION_SELECT_ALL));
      // TODO: Unicode and orientiation

      popup.show(component, x, y);
    }
  }

  private void showPopupMenu(JTextComponent component, MouseEvent me) {
    showPopupMenu(component, me.getX(), me.getY());
  }

  private void moveCaret(JTextComponent component, MouseEvent me) {
    final int index = component.viewToModel(me.getPoint());
    if(index >= 0) {
      component.select(index, index);
    }
  }

  private void updateActionEnablement() {
    boolean selectedText = m_component.getSelectionEnd() - m_component.getSelectionStart() > 0;
    boolean containsText = m_component.getDocument().getLength() > 0;
    boolean editable = m_component.isEditable();
    boolean copyProtected = (m_component instanceof JPasswordField);
    boolean dataOnClipboard = false;
    try {
      dataOnClipboard = m_component.getToolkit().getSystemClipboard().getContents(null) != null;
    } catch (Exception e) {
      // ignore
    }

    ActionMap map = getActionMap();
    map.get(ACTION_UNDO).setEnabled(editable && m_lastUndoableEdit != null && m_lastUndoableEdit.canUndo());
    map.get(ACTION_CUT).setEnabled(!copyProtected && editable && selectedText);
    map.get(ACTION_COPY).setEnabled(!copyProtected && selectedText);
    map.get(ACTION_PASTE).setEnabled(editable && dataOnClipboard);
    map.get(ACTION_DELETE).setEnabled(editable && selectedText);
    map.get(ACTION_SELECT_ALL).setEnabled(containsText);
  }

  private Action createDelegateAction(Action base, String name) {
    Action delegateAction = new DelegateAction(base);
    resAction(delegateAction, name);

    return delegateAction;
  }

  /**
   * An action which delegates to another action, but which can have an
   * independent set of properties (e.g. name)
   */
  private class DelegateAction extends AbstractAction {
    private final Action m_baseAction;

    public DelegateAction(Action baseAction) {
      m_baseAction = baseAction;
    }

    public void actionPerformed(ActionEvent ae) {
      m_baseAction.actionPerformed(ae);
    }
  }


  class UndoAction extends AbstractAction {
    UndoAction() {
      super(ACTION_UNDO);
    }

    public void actionPerformed(ActionEvent ae) {
      if (m_lastUndoableEdit != null && m_lastUndoableEdit.canUndo()) {
        m_lastUndoableEdit.undo();
      }
    }
  }


  public static void resAction(Action action, String text) {
    StringBuffer labelBuffer = new StringBuffer(text.length());
    Integer mnemonic = null;
    char lastChar = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      if (lastChar == '&') {
        if (c == '&') {
          // An escaped ampersand
          labelBuffer.append(c);
        } else if (mnemonic == null) {
          // A mnemonic
          mnemonic = new Integer(c);
          labelBuffer.append(c);
        }
      } else {
        if (c != '&') {
          // An ordinary character
          labelBuffer.append(c);
        }
      }
      lastChar = c;
    }

    if (mnemonic != null) {
      action.putValue(Action.MNEMONIC_KEY, mnemonic);
    }
    action.putValue(Action.NAME, labelBuffer.toString());
  }
}