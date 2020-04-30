package com.almworks.util.components;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author dyoma
 */
public class Separator extends JComponent {
  private final Insets mySpacing = new Insets(3, 5, 3, 5);
  private int myLineWidth = 1;

  public Dimension getPreferredSize() {
    Dimension result = new Dimension(AwtUtil.getInsetWidth(mySpacing), AwtUtil.getInsetHeight(mySpacing) + myLineWidth);
    Border border = getBorder();
    if (border != null)
      AwtUtil.addInsets(result, border.getBorderInsets(this));
    return result;
  }

  public Dimension getMaximumSize() {
    Dimension result = getPreferredSize();
    result.width = Short.MAX_VALUE;
    return result;
  }

  public Dimension getMinimumSize() {
    Dimension result = getPreferredSize();
    result.width = AwtUtil.getInsetWidth(mySpacing) + 2;
    return result;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    g.setColor(getForeground());
    g.drawRect(mySpacing.left, mySpacing.top, getWidth() - AwtUtil.getInsetWidth(mySpacing), myLineWidth - 1);
  }

  public int getLineWidth() {
    return myLineWidth;
  }

  public void setLineWidth(int lineWidth) {
    myLineWidth = lineWidth;
  }
}
