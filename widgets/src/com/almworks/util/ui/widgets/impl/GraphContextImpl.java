package com.almworks.util.ui.widgets.impl;

import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.GraphContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

final class GraphContextImpl extends CellContextImpl implements GraphContext {
  private static final String DOTS = "\u2026";
  private final Graphics2D myGraphics;
  private final Rectangle myClip;
  private final Rectangle myCurrentClip = new Rectangle();
  private int myCurrentTranslateX = 0;
  private int myCurrentTranslateY = 0;
  private boolean myCurrentInitialized = false;
  private boolean myCanReuseGraphics = false;
  private Graphics2D myCurrentGraphics = null;
  private int myLastTranslateX = 0;
  private int myLastTranslateY = 0;

  public GraphContextImpl(HostComponentState<?> state, Graphics2D graphics, Rectangle clip) {
    super(state);
    myGraphics = graphics;
    myClip = clip;
  }

  public boolean prepare(HostCellImpl cell, Rectangle clippedBounds) {
    setCurrentCell(cell);
    if (!myCanReuseGraphics) disposeGraphics();
    else {
      myCurrentInitialized = false;
      if (myCurrentGraphics != null) {
        myCurrentGraphics.translate(-myLastTranslateX, -myLastTranslateY);
        myLastTranslateX = 0;
        myLastTranslateY = 0;
        myCurrentGraphics.setClip(null);
      }
    }
    myCurrentTranslateX = cell.getHostX();
    myCurrentTranslateY = cell.getHostY();
    myCurrentClip.setBounds(clippedBounds);
    AwtUtil.intersection(myCurrentClip, myClip, myCurrentClip);
    return !myCurrentClip.isEmpty();
  }

  private void disposeGraphics() {
    if (myCurrentGraphics != null) {
      myCurrentGraphics.dispose();
      myCurrentGraphics = null;
    }
    myCurrentInitialized = false;
  }

  public Graphics2D getGraphics() {
    Graphics2D g = priGetGraphics();
    myCanReuseGraphics = false;
    return g;
  }

  @Override
  public Rectangle getLocalClip(@Nullable Rectangle target) {
    if (target == null) target = new Rectangle();
    target.setBounds(myCurrentClip);
    target.translate(-myCurrentTranslateX, -myCurrentTranslateY);
    return target;
  }

  @Override
  public void setFontStyle(int style) {
    Graphics2D g = priGetGraphics();
    Font font = g.getFont();
    if (font == null) return;
    if (style == font.getStyle()) return;
    g.setFont(font.deriveFont(style));
    myCanReuseGraphics = false;
  }

  private Graphics2D priGetGraphics() {
    if (myCurrentGraphics == null) {
      myCurrentGraphics = (Graphics2D) myGraphics.create();
      myCanReuseGraphics = true;
      AwtUtil.applyRenderingHints(myCurrentGraphics);
    }
    if (!myCurrentInitialized) {
      myLastTranslateX = myCurrentTranslateX;
      myLastTranslateY = myCurrentTranslateY;
      myCurrentGraphics.translate(myCurrentTranslateX, myCurrentTranslateY);
      myCurrentGraphics.setClip(myCurrentClip.x - myCurrentTranslateX, myCurrentClip.y - myCurrentTranslateY, myCurrentClip.width, myCurrentClip.height);
      JComponent component = getHost().getHost().getHostComponent();
      myCurrentGraphics.setColor(component.getForeground());
      myCurrentGraphics.setFont(component.getFont());
      myCurrentInitialized = true;
    }
    return myCurrentGraphics;
  }

  public void dispose() {
    disposeGraphics();
    myCurrentInitialized = false;
    myCanReuseGraphics = false;
    myLastTranslateX = 0;
    myLastTranslateY = 0;
  }

  @Override
  public void drawRect(Rectangle rect) {
    drawRect(rect.x, rect.y, rect.width, rect.height);
  }

  @Override
  public void fillRect(Rectangle rect) {
    fillRect(rect.x, rect.y, rect.width, rect.height);
  }

  @Override
  public void fillRect(int x, int y, int width, int height) {
    priGetGraphics().fillRect(x, y, width, height);
  }

  @Override
  public void drawRect(int x, int y, int width, int height) {
    priGetGraphics().drawRect(x, y, width - 1, height - 1);
  }

  @Override
  public void setColor(Color color) {
    priGetGraphics().setColor(color);
  }

  @Override
  public FontMetrics getFontMetrics() {
    return priGetGraphics().getFontMetrics();
  }

  @Override
  public void drawString(int x, int y, String text) {
    priGetGraphics().drawString(text, x, y);
  }

  @Override
  public void drawTrancatableString(int x, int y, String text) {
    FontMetrics fontMetrics = getFontMetrics();
    int width = fontMetrics.stringWidth(text);
    int cellWidth = getWidth();
    Graphics2D g = priGetGraphics();
    if (width <= cellWidth) {
      g.drawString(text, x, y);
      return;
    }
    int dotsWidth = fontMetrics.stringWidth(DOTS);
    if (dotsWidth >= cellWidth) {
      g.drawString(DOTS, x, y);
      return;
    }
    String truncedText = TextUtil.truncate(text, fontMetrics, cellWidth - dotsWidth);
    g.drawString(truncedText + DOTS, x, y);
  }
}
