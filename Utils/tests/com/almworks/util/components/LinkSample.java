package com.almworks.util.components;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author : Dyoma
 */
public class LinkSample {
  public static void main(String[] args) {
    final Link link = new Link();
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(link, "Pressed");
      }
    };
    action.putValue(Action.NAME, "a name");
    link.setAction(action);
    showFrame(link, "");
  }

  public static JFrame showFrame(final JComponent link, String title) {
    JFrame frame = new JFrame();
    frame.setTitle(title);
    frame.getContentPane().add(link);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    return frame;
  }
}
