package com.almworks.util.ui;

import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.images.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WelcomeDemo {
  private final JFrame myFrame;
  private JButton myContent;
  private Welcome myWelcome;

  public WelcomeDemo() {
    LAFUtil.initializeLookAndFeel();
    myFrame = new JFrame("WelcomeDemo");
    myContent = new JButton("Press Button To Show Welcome Screen");
    myContent.setToolTipText("THIS IS A TOOLTIP");
    myWelcome = new Welcome(myContent);
    myFrame.getContentPane().add(myWelcome.getComponent());
    myFrame.setSize(500, 450);
    myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  private void run() {
    myFrame.show();
    myContent.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myWelcome.setWelcomeVisible(true);
        myWelcome.setWelcomeText(
          "<html><b>Hello!</b><br><br>This is a welcome screen. myContent = new JButton(\"Press Button To Show Welcome Screen\"); ");
        myWelcome.setActions(new Action[]{
          new AbstractAction() {
            {
              putValue(NAME, "Close Welcome Screen");
              putValue(SMALL_ICON, Icons.ACTION_GENERIC_REMOVE);
            }

            public void actionPerformed(ActionEvent e) {
              myWelcome.setWelcomeVisible(false);
            }
          },
          new AbstractAction() {
            {
              putValue(NAME, "Make a blooper");
              putValue(SMALL_ICON, Icons.ACTION_CREATE_NEW_ITEM);
            }
            public void actionPerformed(ActionEvent e) {
              // bloop
            }
          },
          new AbstractAction() {
            {
              putValue(NAME, "<html>Very very long description of an action which is not going to happen anyway but " +
                "is being described because we need a long description, yeah, and don't forget about the bubbles.");
              putValue(SMALL_ICON, Icons.NODE_FOLDER_OPEN);
            }
            public void actionPerformed(ActionEvent e) {
              // bloop
            }
          }
        });
      }
    });
  }

  public static void main(String[] args) {
    new WelcomeDemo().run();
  }
}
