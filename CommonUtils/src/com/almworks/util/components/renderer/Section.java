package com.almworks.util.components.renderer;

import com.almworks.util.Env;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.TextGraphicsUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
class Section implements CanvasSection, CanvasElement {
  private static final String DOTS = "\u2026";

  private final CanvasElement myParent;
  private final InheritedAttributes myAttributes;
  private final StringBuffer myText = new StringBuffer();

  private String myLastText = null;
  private Dimension myCachedSize = null;

  public Section(CanvasElement parent) {
    myParent = parent;
    myAttributes = new InheritedAttributes(myParent.getAttributes());
  }

  public CanvasSection appendInt(int value) {
    myText.append(value);
    myLastText = null;
    myCachedSize = null;
    return this;
  }

  @Override
  public CanvasSection appendLong(long value) {
    myText.append(value);
    myLastText = null;
    myCachedSize = null;
    return this;
  }

  public boolean isEmpty() {
    return myAttributes.isEmpty() && myText.length() == 0;
  }

  public CanvasSection appendText(String text) {
    myLastText = myText.length() == 0 ? text : null;
    myText.append(text);
    myCachedSize = null;
    return this;
  }

  public void setForeground(Color foreground) {
    myAttributes.myForeground = foreground;
  }

  public void setBackground(Color background) {
    myAttributes.myBackground = background;
  }

  public void setBorder(Border border) {
    myAttributes.myBorder = border;
    myCachedSize = null;
  }

  public CanvasSection setFontStyle(int style) {
    myAttributes.myFontStyle = style;
    myCachedSize = null;
    return this;
  }

  public void copyTo(Canvas canvas) {
    CanvasSection section = canvas.newSection();
    copyAttributes(section);
    section.appendText(getText());
  }

  public void copyAttributes(CanvasSection section) {
    myAttributes.copyTo(section);
  }

  public Dimension getCachedSize(CanvasComponent component) {
    if (myCachedSize == null) {
      FontMetrics fontMetrics = component.getComponent().getFontMetrics(getFont(component));
      int width = fontMetrics.stringWidth(getText());
      Dimension dimension = new Dimension(width, fontMetrics.getHeight());
      Border border = getBorder();
      if (border != null) {
        Insets insets = border.getBorderInsets(component.getComponent());
        AwtUtil.addInsets(dimension, insets);
      }
      myCachedSize = dimension;
    }
    return myCachedSize;
  }

  @NotNull
  public String getText() {
    if (myLastText == null)
      myLastText = myText.toString();
    return myLastText;
  }

  private Border getBorder() {
    return myAttributes.getBorder();
  }

  private Font getFont(@NotNull CanvasComponent component) {
    return component.getDerivedFont(myAttributes.getFontStyle());
  }

  public void paint(Rectangle rect, Graphics g, CanvasComponent component) {
    component.setCurrentElement(this);
    Border border = getBorder();
    Graphics borderGraphics = Env.isMac() && border != null ? g.create() : g;
    int x = rect.x;
    int y = rect.y;
    try {
      Color bg = myAttributes.getBackground();
      if (bg != null) {
        g.setColor(bg);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
      }
      if (border != null) {
        border.paintBorder(component.getComponent(), borderGraphics, rect.x, rect.y, rect.width, rect.height);
        Insets insets = border.getBorderInsets(component.getComponent());
        x += insets.left;
        y += insets.top;
      }
    } finally {
      if (borderGraphics != g) {
        borderGraphics.dispose();
      }
    }
    Dimension size = getCachedSize(component);
    y += (rect.height - size.height) / 2;
    int textWidth = rect.x + rect.width - x;
    drawText(g, x, y, textWidth, component);
  }

  private Pattern getPattern(CanvasComponent canvasComponent) {
    return canvasComponent.getHighlightPattern();
  }

  private void drawText(Graphics g, int x, int y, int textWidth, CanvasComponent component) {
    Pattern pattern = getPattern(component);
    Font font = getFont(component);
    FontMetrics fontMetrics = component.getComponent().getFontMetrics(font);
    String text = getText();
    int width = fontMetrics.stringWidth(text);
    g.setFont(font);
    g.setColor(myAttributes.getForeground());
    int baseLine = y + fontMetrics.getAscent();
    if (width <= textWidth) {
      TextGraphicsUtil.drawMatchedTextHighlight(g, x, pattern, fontMetrics, text, y);
      g.drawString(text, x, baseLine);
      return;
    }
    int dotsWidth = fontMetrics.stringWidth(DOTS);
    if (dotsWidth >= textWidth) {
      g.drawString(DOTS, x, baseLine);
      return;
    }
    String truncedText = TextUtil.truncate(text, fontMetrics, textWidth - dotsWidth);
    TextGraphicsUtil.drawMatchedTextHighlight(g, x, pattern, fontMetrics, truncedText, y);
    g.drawString(truncedText + DOTS, x, baseLine);
  }

  public TextAttributes getAttributes() {
    return myAttributes;
  }

  public void clear() {
    myAttributes.clear();
    myText.setLength(0);
    myLastText = null;
  }
}
