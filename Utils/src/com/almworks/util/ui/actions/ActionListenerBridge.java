package com.almworks.util.ui.actions;

import org.almworks.util.Failure;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author : Dyoma
 */
public class ActionListenerBridge extends AbstractAction {
  private final AnActionListener myListener;

  private ActionListenerBridge(AnActionListener listener) {
    assert listener != null;
    myListener = listener;
  }

  public void actionPerformed(ActionEvent e) {
    try {
      myListener.perform(new DefaultActionContext(e));
    } catch (CantPerformException e1) {
      throw new Failure(e1);
    }
  }

  public static ActionListener listener(AnActionListener listener) {
    return new ActionListenerBridge(listener);
  }

  public static Action action(AnActionListener listener) {
    return new ActionListenerBridge(listener);
  }
}
