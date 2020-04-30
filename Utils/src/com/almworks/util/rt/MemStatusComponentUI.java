package com.almworks.util.rt;

import com.almworks.util.ui.ColorUtil;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/**
 * Default UI for MemStatusComponent
 *
 * @author Vasya
 */
public class MemStatusComponentUI extends ComponentUI {
  private static final ComponentUI INSTANCE = new MemStatusComponentUI();
  private static final int MEGABYTE = 1 << 20;

  public static ComponentUI createUI(JComponent c) {
    return INSTANCE;
  }

  public void installUI(JComponent c) {
    super.installUI(c);
    LookAndFeel.installColorsAndFont(c, "TextField.background", "TextField.foreground", "Label.font");
    MemStatusComponent memStatus = ((MemStatusComponent)c);

    // todo a better way and better colors
    c.setBackground(new ColorUIResource(ColorUtil.between(c.getForeground(), c.getBackground(), .75F)));

    Color color = memStatus.getIndicatorColor();
    if (color == null || color instanceof UIResource)
      memStatus.setIndicatorColor(new ColorUIResource(ColorUtil.between(c.getForeground(), c.getBackground(), .5F)));

    color = memStatus.getPeakColor();
    if (color == null || color instanceof UIResource)
      memStatus.setPeakColor(new ColorUIResource(ColorUtil.between(c.getForeground(), c.getBackground(), .75F)));
  }

  public void paint(Graphics g, JComponent c) {
    MemStatusComponent memStatus = (MemStatusComponent) c;
    MemoryState state = memStatus.getMemoryState();
    Insets insets = c.getInsets();
    String label = visualBytes(state.getUsedMemory()) + " of " + visualBytes(state.getMaxMemory());
    Font font = c.getFont();
    LineMetrics lineMetrics = font.getLineMetrics(label, new FontRenderContext(null, false, false));
    int labelWidth = c.getFontMetrics(font).stringWidth(label);
//    Rectangle2D r = fontMetrics.getStringBounds(label, g);

    int w = c.getWidth() - insets.left - insets.right;
    int h = c.getHeight() - insets.top - insets.bottom;

    int busyWidth = (int) (state.getUsedRatio() * w);
    int peakWidth = (int) (state.getPeakRatio() * w);

    g.setColor(memStatus.getIndicatorColor());
    g.fillRect(insets.left, insets.top, busyWidth, h);

    g.setColor(memStatus.getPeakColor());
    g.fillRect(insets.left + busyWidth, insets.top, peakWidth - busyWidth, h);

    g.setColor(c.getBackground());
    g.fillRect(insets.left + peakWidth, insets.top, w - peakWidth, h);

    g.setColor(c.getForeground());
    g.drawString(label,
      insets.left + (int) ((w - labelWidth) / 2),
      insets.top + (int)(((h + lineMetrics.getAscent() + lineMetrics.getDescent()) / 2) - lineMetrics.getDescent()));
//    c.setToolTipText("Memory is used on " + (int) (busyRatio * 100) + '%');
  }

  protected String visualBytes(long value) {
    return (int)(Math.floor((float) value / MEGABYTE)) + "M";
  }
}
