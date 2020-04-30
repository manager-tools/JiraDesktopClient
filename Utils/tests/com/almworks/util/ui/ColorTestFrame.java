package com.almworks.util.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ColorTestFrame {
  private JButton myChangeButton;
  private JSpinner myBrightness;
  private JSpinner myBlue;
  private JSpinner myGreen;
  private JSpinner myRed;
  private JPanel myShowPanel;

  private Color myColor;
  private JTextField myColorField;
  private JPanel myWholePanel;

  public static void main(String[] args) {
    new ColorTestFrame().run();
  }

  private void run() {
    JFrame myFrame = new JFrame("colors!");
    myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    myFrame.getContentPane().add(myWholePanel);
    myShowPanel.setLayout(new BorderLayout(4, 4));
    setColor(new Color(0.5F, 0.5F, 0.5F));
    setColor(new Color(0, 0, 0));
    myChangeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        change();
      }
    });
    myFrame.pack();
    myFrame.show();
  }

  private void change() {
    setColor(ColorUtil.transform(myColor, getValue(myRed), getValue(myGreen), getValue(myBlue), getValue(myBrightness)));
  }

  private int getValue(JSpinner spinner) {
    return ((Integer)spinner.getValue()).intValue();
  }

  private void setColor(Color color) {
    myColor = color;
    myShowPanel.setBackground(color);
    myColorField.setText(color.toString());
    myWholePanel.repaint();
  }

}
