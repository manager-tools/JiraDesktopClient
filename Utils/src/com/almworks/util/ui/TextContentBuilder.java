package com.almworks.util.ui;

import com.almworks.util.components.ALabel;
import com.almworks.util.components.Link;
import com.almworks.util.components.URLLink;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TextContentBuilder {
  private final Box myContent;

  public TextContentBuilder(String title, String message) {
    final int gap = 9;
    myContent = Box.createVerticalBox();
    myContent.setBackground(AwtUtil.getPanelBackground());
    myContent.setBorder(new EmptyBorder(0, gap, gap, gap));

    final ALabel header = new ALabel(title);
    UIUtil.adjustFont(header, 1.8F, -1, true);
    header.setForeground(ColorUtil.between(header.getForeground(), Color.BLUE, 0.3F));
    final ALabel label = new ALabel(message);
    label.setPreferredWidth(UIUtil.getColumnWidth(label) * 40);

    myContent.add(header);
    myContent.add(Box.createVerticalStrut(UIUtil.getLineHeight(label)));
    myContent.add(label);
  }

  public void appendLink(String url, String text, int topGap) {
    final Link link = new URLLink(url, true, text).leftAligned();
    link.setBorder(new EmptyBorder(topGap, 0, 0, 0));
    myContent.add(link);
  }

  public Box getComponent() {
    for (final Component c : myContent.getComponents()) {
      ((JComponent) c).setAlignmentX(0f);
    }
    return myContent;
  }
}
