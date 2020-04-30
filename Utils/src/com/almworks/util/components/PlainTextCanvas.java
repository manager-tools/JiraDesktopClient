package com.almworks.util.components;

import com.almworks.util.TODO;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.RendererContext;
import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

/**
 * @author dyoma
 */
public class PlainTextCanvas implements Canvas, Canvas.Line {
  private final List<PlainTextSection> mySections = Collections15.arrayList();
  private int myFontStyle = Font.PLAIN;

  public void setIcon(Icon icon) {
  }

  public void setIconMargin(Insets margin) {
  }

  public void setToolTipText(String s) {
  }

  public void setEnabled(boolean enabled) {
  }

  public void setBackground(Color bg) {
  }

  public void setForeground(Color fg) {
  }

  public void setCanvasBorder(Border border) {
  }

  public Border getCanvasBorder() {
    return null;
  }

  public void setCanvasBackground(Color background) {
  }

  public Color getCanvasBackground() {
    return null;
  }

  public void appendText(String text) {
    getCurrentSection().appendText(text);
  }

  public void appendInt(int value) {
    getCurrentSection().appendInt(value);
  }

  @Override
  public void appendLong(long value) {
    getCurrentSection().appendLong(value);
  }

  public void copyAttributes(Canvas canvas) {
  }

  public void copyTo(Canvas canvas) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < mySections.size(); i++) {
      PlainTextSection section = mySections.get(i);
      section.copyTo(canvas);
    }
  }

  public void setFontStyle(int style) {
    myFontStyle = style;
  }

  public void clear() {
    mySections.clear();
    myFontStyle = Font.PLAIN;
  }

  public void setFullyOpaque(boolean opaque) {
  }

  public Line newLine() {
    throw TODO.notImplementedYet();
  }

  public CanvasSection newSection() {
    PlainTextSection result = new PlainTextSection(this);
    mySections.add(result);
    return result;
  }

  public CanvasSection emptySection() {
    CanvasSection section = getCurrentSection();
    if (section.isEmpty())
      return section;
    return newSection();
  }

  public CanvasSection getCurrentSection() {
    if (mySections.isEmpty())
      return newSection();
    return mySections.get(mySections.size() - 1);
  }

  public void renderOn(Canvas canvas, CellState state) {
    copyTo(canvas);
  }

  @Override
  public Line getCurrentLine() {
    return this;
  }

  @Override
  public CanvasSection[] getSections() {
    return mySections.toArray(new CanvasSection[mySections.size()]);
  }

  @Override
  public void setHorizontalAlignment(float alignment) {
  }

  public String getText() {
    StringBuffer buffer = new StringBuffer();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < mySections.size(); i++) {
      PlainTextSection section = mySections.get(i);
      buffer.append(section.getText());
    }
    return buffer.toString();
  }

  public void getSize(RendererContext context, Dimension dimension) {
    int width = 0;
    int height = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < mySections.size(); i++) {
      PlainTextSection section = mySections.get(i);
      section.getSize(context, dimension);
      width += dimension.width;
      height = Math.max(height, dimension.height);
    }
    dimension.width = width;
    dimension.height = height;
  }

  public int getFontStyle() {
    return myFontStyle;
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < mySections.size(); i++) {
      PlainTextSection section = mySections.get(i);
      x += section.paint(g, x, y, context);
    }
  }

  public static String renderText(CanvasRenderable renderable) {
    PlainTextCanvas canvas = new PlainTextCanvas();
    renderable.renderOn(canvas, CellState.LABEL);
    return canvas.getText();
  }

  public static <T> String renderText(T item, CanvasRenderer<? super T> renderer) {
    PlainTextCanvas canvas = new PlainTextCanvas();
    renderer.renderStateOn(CellState.LABEL, canvas, item);
    return canvas.getText();
  }

  private static class PlainTextSection implements CanvasSection {
    private final StringBuilder myBuffer = new StringBuilder();
    private final PlainTextCanvas myParent;
    private int myFontStyle = -1;
    private String myLastText = null;

    public PlainTextSection(PlainTextCanvas parent) {
      myParent = parent;
    }

    public CanvasSection appendText(String text) {
      myLastText = null;
      myBuffer.append(text);
      return this;
    }

    public CanvasSection appendInt(int value) {
      myLastText = null;
      myBuffer.append(value);
      return this;
    }

    @Override
    public CanvasSection appendLong(long value) {
      myLastText = null;
      myBuffer.append(value);
      return this;
    }

    public boolean isEmpty() {
      return myBuffer.length() == 0;
    }

    public void setForeground(Color foreground) {
    }

    public void setBackground(Color background) {
    }

    public void setBorder(Border border) {
    }

    public CanvasSection setFontStyle(int style) {
      myFontStyle = style;
      return this;
    }

    public void copyTo(Canvas canvas) {
      canvas.emptySection().appendText(getText());
    }

    public String getText() {
      if (myLastText == null)
        myLastText = myBuffer.toString();
      return myLastText;
    }

    public void copyAttributes(CanvasSection section) {
    }

    public void getSize(RendererContext context, Dimension dimension) {
      context.getStringWidth(getText(), getFontStyle());
    }

    private int getFontStyle() {
      return myFontStyle != -1 ? myFontStyle : myParent.getFontStyle();
    }

    public int paint(Graphics g, int x, int y, RendererContext context) {
      int fontStyle = getFontStyle();
      String text = getText();
      g.setFont(context.getFont(fontStyle));
      g.drawString(text, x, y + context.getFontBaseLine(fontStyle));
      return context.getStringWidth(text, fontStyle);
    }
  }


  public static class ThreadLocalFactory extends ThreadLocal<PlainTextCanvas> {
    protected PlainTextCanvas initialValue() {
      return new PlainTextCanvas();
    }
  }
}
