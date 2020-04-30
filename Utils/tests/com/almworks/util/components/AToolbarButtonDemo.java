package com.almworks.util.components;

import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.images.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * :todoc:
 *
 * @author sereda
 */
public class AToolbarButtonDemo {
  private final JFrame myFrame;
  private final JScrollPane myTextView;
  private final JTextArea myTextPane;
  private final AToolbar myToolbar;

  public AToolbarButtonDemo() {
    LAFUtil.initializeLookAndFeel();

    myFrame = new JFrame("AToolbarButton Demo");
    myTextPane = new JTextArea(20, 60);
    myTextView = new JScrollPane(myTextPane);
    myToolbar = new AToolbar();
    JPanel panel = new JPanel(new BorderLayout(2, 2));
    myFrame.getContentPane().add(panel);
    panel.add(myTextView, BorderLayout.CENTER);
    panel.add(myToolbar, BorderLayout.NORTH);
    myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  private void run() {
    AToolbarButton b;
    b = createButton("Undo", Icons.NODE_FOLDER_CLOSED);
    b.setEnabled(false);
    myToolbar.add(b);
    b = createButton("Redo", Icons.NODE_FOLDER_OPEN);
    b.setSelected(true);
    myToolbar.add(b);
    myToolbar.add(createButton(null, Icons.ACTION_SYNCHRONIZE_FULL));
    myToolbar.add(createButton(null, Icons.ACTION_SYNCHRONIZE_THIS));
    b = createButton(null, Icons.ACTION_SYNCHRONIZE_DOWNLOAD_ONLY);
    b.setEnabled(false);
    myToolbar.add(b);
    myToolbar.add(new JLabel("here"));

    myFrame.pack();
    myFrame.show();
  }

  public static void main(String[] args) {
    new AToolbarButtonDemo().run();
  }

  public static AToolbarButton createButton(String text, Icon icon) {
    AToolbarButton button = new AToolbarButton();
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
      }
    };
    action.putValue(Action.NAME, text);
    action.putValue(Action.SMALL_ICON, icon);
    button.setAction(action);
    return button;
  }
}
