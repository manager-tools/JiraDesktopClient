package com.almworks.spellcheck.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class DefaultSpellHighlighter implements Highlighter.HighlightPainter {
  public static final Highlighter.HighlightPainter DEFAULT = new DefaultSpellHighlighter(Color.RED, new BasicStroke(0.8F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10F, new float[]{1f, 1f}, 0f));

  private final Color myColor;
  @Nullable
  private final Stroke myStroke;

  public DefaultSpellHighlighter(@NotNull Color color, @Nullable Stroke stroke) {
    myColor = color;
    myStroke = stroke;
  }

  @Override
  public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
    Rectangle alloc = bounds.getBounds();
    try {
      TextUI mapper = c.getUI();
      Rectangle p0 = mapper.modelToView(c, offs0);
      Rectangle p1 = mapper.modelToView(c, offs1);
      int ascent = c.getFontMetrics(c.getFont()).getAscent();

      g.setColor(myColor);
      Stroke saveStroke = null;
      if (myStroke != null) {
        Graphics2D g2 = (Graphics2D) g;
        saveStroke = g2.getStroke();
        g2.setStroke(myStroke);
      }
      if (p0.y == p1.y) {
        // same line, render a rectangle
        Rectangle r = p0.union(p1);
        drawLine(ascent, g, r.x, r.y, r.width);
      } else {
        // different lines
        int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
        drawLine(ascent, g, p0.x, p0.y, p0ToMarginWidth);
        int fullY = p0.y + p0.height;
        while (fullY < p1.y) drawLine(ascent, g, alloc.x, fullY, alloc.width);
        drawLine(ascent, g, alloc.x, p1.y, p1.x - alloc.x);
      }
      if (saveStroke != null) ((Graphics2D) g).setStroke(saveStroke);
    } catch (BadLocationException e) {
      // can't render
    }
  }

  private void drawLine(int ascent, Graphics g, int x, int y, int width) {
    g.drawLine(x, y + ascent + 1, x + width, y + ascent + 1);
  }
}
