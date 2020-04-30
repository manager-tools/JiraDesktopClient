package com.almworks.util.components;

import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.PaintUtil;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

public class OneLineLabel extends JLabel {
  private boolean myHtmlContent = false;

  public Dimension getPreferredSize() {
    FontMetrics metrics = getFontMetrics(getFont());
    return new Dimension(0, metrics.getAscent() + metrics.getDescent());
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  protected void paintComponent(Graphics g) {
    String text = getText();
    if (text == null || text.length() == 0)
      return;
    AwtUtil.applyRenderingHints(g);
    Dimension size = getSize();
    Insets insets = getInsets();
    int width = size.width - insets.left - insets.right;
    int height = size.height - insets.top - insets.bottom;
    Font font = getFont();
    FontMetrics metrics = getFontMetrics(font);
    String[] lines = TextUtil.getPlainTextLines(text, width, 1, metrics, myHtmlContent, true);
    assert lines.length < 2;
    String s = lines.length > 0 ? lines[0].trim() : null;
    if (s != null) {
      PaintUtil.viewR.setBounds(insets.left, insets.top, width, height);
      PaintUtil.iconR.setBounds(0, 0, 0, 0);
      PaintUtil.textR.setBounds(0, 0, 0, 0);
      s = SwingUtilities.layoutCompoundLabel(this, metrics, s, null, CENTER, LEFT, CENTER, CENTER, PaintUtil.viewR,
        PaintUtil.iconR, PaintUtil.textR, 0);
      g.setFont(font);
      g.setColor(getForeground());
      g.drawString(s, PaintUtil.textR.x, PaintUtil.textR.y + metrics.getAscent());
    }
  }

  public OneLineLabel setHtmlContent(boolean htmlContent) {
    if (htmlContent != myHtmlContent) {
      myHtmlContent = htmlContent;
      if (isDisplayable() && isVisible())
        repaint();
    }
    return this;
  }
}
