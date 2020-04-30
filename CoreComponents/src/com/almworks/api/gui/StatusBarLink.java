package com.almworks.api.gui;

import com.almworks.util.components.Link;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;

public class StatusBarLink extends Link {
  private JToolTip mySampleToolTip = new JToolTip();
  private boolean myAugmentBorder = false;

  public StatusBarLink() {
    setForeground(UIManager.getColor("Label.foreground"));
  }

  public void setToolTipText(String text) {
    super.setToolTipText(text);
    myAugmentBorder = text != null && text.startsWith("<html>");
  }

  public JToolTip createToolTip() {
    JToolTip toolTip = super.createToolTip();
    if (myAugmentBorder) {
      Border border = toolTip.getBorder();
      border = new CompoundBorder(border, new EmptyBorder(0, 4, 0, 4));
      toolTip.setBorder(border);
    }
    return toolTip;
  }

  public Point getToolTipLocation(MouseEvent event) {
    mySampleToolTip.setTipText(getToolTipText());
    int height = mySampleToolTip.getPreferredSize().height;
    return new Point(0, -height - 2);
  }
}
