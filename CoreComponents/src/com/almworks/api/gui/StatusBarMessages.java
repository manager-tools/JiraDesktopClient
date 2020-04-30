package com.almworks.api.gui;

import com.almworks.util.images.Icons;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class StatusBarMessages {
  public static StatusBarMessage createWarning(StatusBar statusBar, ActionListener listener, String text, String tooltip) {
    final StatusBarMessage message = new StatusBarMessage(statusBar, listener, text, Icons.ATTENTION, tooltip);
    final JComponent c = message.getComponent();
    c.setOpaque(true);
    c.setBackground(ColorUtil.between(Color.YELLOW, AwtUtil.getPanelBackground(), 0.5F));
    c.setForeground(Color.BLACK);
    return message;
  }

  public static StatusBarMessage createError(StatusBar statusBar, ActionListener listener, String text, String tooltip) {
    final StatusBarMessage message = new StatusBarMessage(statusBar, listener, text, Icons.ATTENTION, tooltip);
    final JComponent c = message.getComponent();
    c.setOpaque(true);
    c.setBackground(ColorUtil.between(Color.RED, AwtUtil.getPanelBackground(), 0.2F));
    c.setForeground(Color.WHITE);
    return message;
  }

  public static StatusBarMessage createDiagnosticMode(StatusBar statusBar, ActionListener listener, String text, String tooltip) {
    final StatusBarMessage message = createError(statusBar, listener, text, tooltip);
    message.setIcon(Icons.ATTENTION_BLINKING.getIcon());
    return message;
  }
}
