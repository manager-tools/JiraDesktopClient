package com.almworks.util.ui;

import com.almworks.util.Env;
import com.almworks.util.ui.swing.DocumentUtil;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 */
public class TextImproveUtil {
  public static final TextImproveUtil INSTANCE = new TextImproveUtil();

  private final static String BSKEY = "ctrlBackSpace";
  private final static String DELKEY = "ctrlDel";


  private final static AbstractAction CTRL_BACKSPACE_ACTION = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() instanceof JTextComponent) {
        runCtrlBackspace(((JTextComponent) e.getSource()));
      }
    }
  };
  private final static AbstractAction CTRL_DELETE_ACTION = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() instanceof JTextComponent) {
        runCtrlDelete(((JTextComponent) e.getSource()));
      }
    }
  };

  public static void improveTextOverMaps(final JTextComponent text) {
    final InputMap inputMap = text.getInputMap();
    final ActionMap actionMap = text.getActionMap();
    int modifier = (Env.isMac()) ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, modifier), BSKEY);

    actionMap.put(BSKEY, CTRL_BACKSPACE_ACTION);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, modifier), DELKEY);

    actionMap.put(DELKEY, CTRL_DELETE_ACTION);
  }


  private static void runCtrlDelete(JTextComponent text) {
    String s = DocumentUtil.getDocumentText(text.getDocument());
    int position = text.getCaretPosition();
    int i;
    i = findWordEnd(s, position);
    final int length = i - position;
    try {
      text.getDocument().remove(position, length);
    } catch (BadLocationException e1) {
      // ignore
    }
  }

  private static void runCtrlBackspace(JTextComponent text) {
    String s = DocumentUtil.getDocumentText(text.getDocument());
    int position = text.getCaretPosition();
    int i;
    i = findWordStart(s, position);
    final int length = position - i;
    try {
      text.getDocument().remove(i, length);
    } catch (BadLocationException e11) {
      // ignore
    }
  }

  static int findWordEnd(String s, int position) {
    int i = position;
    final int len = s.length();
    while (i < len) {
      char c = s.charAt(i++);
      if (!Character.isLetterOrDigit(c)) {
        break;
      }
    }
    return i;
  }

  static int findWordStart(String s, int position) {
    int i = position;
    while (i > 0) {
      i--;
      char c = s.charAt(i);
      if (!Character.isLetterOrDigit(c)) {
        break;
      }
    }
    return i;
  }
}
