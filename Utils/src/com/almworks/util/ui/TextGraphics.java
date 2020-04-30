package com.almworks.util.ui;

import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class TextGraphics {
  private static TextGraphics INSTANCE;
  private Graphics myGraphics;
  private Font myFont;
  private JComponent myComponent;
  private int myDefaultFontStyle;

  private int myX;
  private int myY;
  private int myCornerX;
  private int myCurrentStyle;
  private final Map<Integer, FontMetrics> myFontMetrics = Collections15.hashMap();
  private Pattern myHighlightPattern;

  public TextGraphics(Graphics graphics, JComponent component) {
    init(graphics, component);
  }

  public void setCorner(int x, int y) {
    myY = y;
    myX = x;
    myCornerX = x;
  }

  public void setFontStyle(int style) {
    if (style != myCurrentStyle)
      myGraphics.setFont(getFontMetrics(style).getFont());
    myCurrentStyle = style;
  }

  public void draw(String str) {
    FontMetrics fontMetrics = getCurrentFontMetrics();
    TextGraphicsUtil.drawMatchedTextHighlight(myGraphics, myX, myHighlightPattern, fontMetrics, str, myY);
    myGraphics.drawString(str, myX, myY + fontMetrics.getAscent());
    myX += fontMetrics.stringWidth(str);
  }

  public FontMetrics getCurrentFontMetrics() {
    return getFontMetrics(myCurrentStyle);
  }

  public void skipPixels(int pixels) {
    myX += pixels;
  }

  public int getLineHeight() {
    return getCurrentFontMetrics().getHeight();
  }

  public void setDefaultStyle() {
    setFontStyle(myDefaultFontStyle);
  }

  public void newLine() {
    myX = myCornerX;
    myY += getLineHeight();
  }

  @NotNull
  private FontMetrics getFontMetrics(int style) {
    FontMetrics fontMetrics = myFontMetrics.get(style);
    if (fontMetrics == null) {
      fontMetrics = myComponent.getFontMetrics(myFont.deriveFont(style));
      assert fontMetrics != null : style;
      myFontMetrics.put(style, fontMetrics);
    }
    return fontMetrics;
  }

  private void init(Graphics graphics, JComponent component) {
    myX = 0;
    myY = 0;
    myCornerX = 0;
    myGraphics = graphics;
    myComponent = component;
    Font newFont = myComponent.getFont();
    if (myFont != null && !Util.equals(myFont.getFontName(), newFont.getFontName()))
      myFontMetrics.clear();
    myFont = newFont;
    myDefaultFontStyle = myFont.getStyle();
    myCurrentStyle = myDefaultFontStyle;
    myGraphics.setFont(myFont);
    myGraphics.setColor(myComponent.getForeground());
  }

  public void dispose() {
    myGraphics = null;
    myComponent = null;
    synchronized(TextGraphics.class) {
      if (INSTANCE == null)
        INSTANCE = this;
    }
  }

  public static TextGraphics getInstance(Graphics g, JComponent component) {
    if (INSTANCE != null)
      synchronized (TextGraphics.class) {
        if (INSTANCE != null) {
          TextGraphics result = INSTANCE;
          INSTANCE = null;
          result.init(g, component);
          return result;
        }
      }
    return new TextGraphics(g, component);
  }

  public void fillLinePixels(Color color, int pixelLength) {
    Color savedColor = myGraphics.getColor();
    myGraphics.setColor(color);
    myGraphics.fillRect(myX, myY, pixelLength, getLineHeight());
    myGraphics.setColor(savedColor);
  }

  public void setForeground(Color color) {
    myGraphics.setColor(color);
  }

  public void setHighlightPattern(Pattern pattern) {
    myHighlightPattern = pattern;
  }
}
