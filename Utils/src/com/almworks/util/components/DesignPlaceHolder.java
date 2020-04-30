package com.almworks.util.components;

import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class DesignPlaceHolder extends JComponent {
  private static final Rectangle rect = new Rectangle();
  private static Color lineColor;
  private static Color textColor;
  private static Font textFont;

  static {
    Color panel = Util.NN(UIManager.getColor("Panel.background"), Color.GRAY);
    lineColor = ColorUtil.between(panel, Color.BLUE, 0.2F);
    textColor = ColorUtil.between(panel, UIUtil.getEditorForeground(), 0.3F);
    Font f = UIManager.getFont("Label.font");
    if (f == null)
      f = Font.decode("Arial-PLAIN-12");
    textFont = f.deriveFont(Font.BOLD);
  }

  private int myDesignColumns = 10;
  private int myDesignRows = 3;

  public DesignPlaceHolder() {
    setOpaque(false);
  }

  public void setComponent(Component c) {
    if (getComponentCount() > 0) {
      removeAll();
    }
    add(c);
    invalidate();
  }

  public void replaceWith(Component c) {
    Container p = getParent();
    if (p == null) {
      assert false : "already replaced " + c;
      Log.warn("already replaced " + c);
      return;
    }
    LayoutManager lm = p.getLayout();
    if (!(lm instanceof FormLayout)) {
      assert false : "unknown layout " + this + " " + lm;
      Log.warn("unknown layout " + this + " " + lm);
      return;
    }
    CellConstraints cc = ((FormLayout) lm).getConstraints(this);
    p.remove(this);
    p.add(c, cc);
  }

  public Component getComponent() {
    return getComponentCount() > 0 ? getComponent(0) : null;
  }

  @Override
  public void layout() {
    Component c = getComponent();
    if (c != null) {
      getBounds(rect);
      c.setBounds(0, 0, rect.width, rect.height);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Component c = getComponent();
    if (c != null)
      return c.getPreferredSize();
    return UIUtil.getRelativeDimension(this, Math.max(1, myDesignColumns), Math.max(1, myDesignRows));
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (getComponentCount() > 0) {
      // all is painted by children
      return;
    }
    AwtUtil.applyRenderingHints(g);
    getBounds(rect);
    String name = getName();
    drawPlaceholder(g, 0, 0, rect.width, rect.height, name);
  }

  public int getDesignColumns() {
    return myDesignColumns;
  }

  public void setDesignColumns(int designColumns) {
    myDesignColumns = designColumns;
  }

  public int getDesignRows() {
    return myDesignRows;
  }

  public void setDesignRows(int designRows) {
    myDesignRows = designRows;
  }

  public static void drawPlaceholder(Graphics g, int x, int y, int width, int height, String name) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setColor(lineColor);
      g2.drawLine(x, y, x + width, y + height);
      g2.drawLine(x, y + height, x + width, y);
      if (name != null && name.length() > 0) {
        g2.setColor(textColor);
        g2.setFont(textFont);
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(name, g2);
        int tx = x + (width - (int) bounds.getWidth()) / 2;
        int ty = y + (height - (int) bounds.getHeight()) / 2 + fm.getAscent();
        g2.drawString(name, tx, ty);
      }
    } finally {
      g2.dispose();
    }
  }
}
