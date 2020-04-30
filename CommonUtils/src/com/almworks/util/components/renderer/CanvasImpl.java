package com.almworks.util.components.renderer;

import com.almworks.util.Env;
import com.almworks.util.collections.ObjectArray;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author dyoma
 */
public class CanvasImpl implements Canvas, CanvasElement {
  private final static Insets DEFAULT_ICON_MARGIN = new Insets(0, 0, 0, 5);

  private Icon myIcon = null;
  /**
   * Not null
   */
  private Insets myIconMargin = DEFAULT_ICON_MARGIN;
  private boolean myIconOpaque = false;
  private final ObjectArray<SectionLine> myLines = ObjectArray.create();
  private final RootAttributes myDefaultAttributes = new RootAttributes();
  private String myTooltip = null;
  private Border myBorder;
  private Color myBackground = null;
  private boolean myClearComponent = false;
  private Font myComponentFont = null;

  public void clear() {
    if (myLines.isEmpty())
      myLines.add(new SectionLine(this));
    else {
      if (myLines.size() > 1)
        myLines.removeTail(1);
      myLines.get(0).clear();
    }
    assert myLines.size() == 1;

    myBorder = null;
    myBackground = null;
    myIcon = null;
    myIconMargin = DEFAULT_ICON_MARGIN;
    myDefaultAttributes.clear();
    myIconOpaque = false;
    myTooltip = null;
    myClearComponent = true;
  }

  public void setForeground(Color foreground) {
    myDefaultAttributes.myForeground = foreground;
  }

  public void setCanvasBorder(Border border) {
    myBorder = border;
  }

  public Border getCanvasBorder() {
    return myBorder;
  }

  public void setCanvasBackground(Color background) {
    myBackground = background;
  }

  public Color getCanvasBackground() {
    return myBackground;
  }

  public void setBackground(@Nullable Color background) {
    myDefaultAttributes.myBackground = background;
  }

  public void setEnabled(boolean enabled) {
    myDefaultAttributes.myEnabled = enabled;
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }

  public void setIconMargin(Insets margin) {
    if (margin == null)
      myIconMargin = DEFAULT_ICON_MARGIN;
    else {
      assert margin.top == 0 : margin;
      assert margin.bottom == 0 : margin;
      myIconMargin = margin;
    }
  }

  public void setFontStyle(int style) {
    myDefaultAttributes.myFontStyle = style;
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

  public void setToolTipText(String s) {
    myTooltip = s;
  }

  public void setFullyOpaque(boolean opaque) {
    if (opaque && myDefaultAttributes.myBackground == null) {
      assert false : "background color is needed for fully opaque canvas";
    }
    myIconOpaque = opaque;
  }

  public CanvasSection newSection() {
    return getCurrentLine().newSection();
  }

  public CanvasSection emptySection() {
    CanvasSection section = getCurrentSection();
    if (section.isEmpty())
      return section;
    return newSection();
  }

  public CanvasSection getCurrentSection() {
    return getCurrentLine().getCurrentSection();
  }

  public SectionLine newLine() {
    SectionLine line = new SectionLine(this);
    myLines.add(line);
    return line;
  }

  public void copyTo(com.almworks.util.components.Canvas canvas) {
    copyAttributes(canvas);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myLines.size(); i++) {
      SectionLine line = myLines.get(i);
      line.copyTo(canvas);
    }
  }

  public void copyAttributes(Canvas canvas) {
    canvas.setFullyOpaque(myIconOpaque);
    canvas.setToolTipText(myTooltip);
    canvas.setCanvasBorder(myBorder);
    canvas.setCanvasBackground(myBackground);
    myDefaultAttributes.copyTo(canvas);
  }

  public void renderOn(Canvas canvas, CellState state) {
    copyTo(canvas);
  }

  @Override
  public SectionLine getCurrentLine() {
    assert !myLines.isEmpty();
    return myLines.get(myLines.size() - 1);
  }

  public void setupProperties(CellState state) {
    setForeground(state.getForeground());
    setBackground(state.getBackground());
    setCanvasBackground(state.getBackground());
    myComponentFont = state.getFont();
    setEnabled(state.isEnabled());
    myBorder = state.getBorder();
    myDefaultAttributes.myFontStyle = Font.PLAIN;
  }

  public String getTooltip() {
    return myTooltip;
  }

  public RootAttributes getAttributes() {
    return myDefaultAttributes;
  }

  private static final Insets TEMP_INSETS = new Insets(0, 0, 0, 0);
  public void paint(Graphics g, Dimension size, CanvasComponent component) {
    component.getComponent().getInsets(TEMP_INSETS);
    int y = TEMP_INSETS.top;
    int x = TEMP_INSETS.left;
    int wholeWidth = size.width;
    int wholeHeight = size.height;
    if (myBackground != null) {
      int insetWidth = AwtUtil.getInsetWidth(TEMP_INSETS);
      int insetsHeight = AwtUtil.getInsetHeight(TEMP_INSETS);
      g.setColor(myBackground);
      g.fillRect(x, y, wholeWidth - insetWidth, wholeHeight - insetsHeight);
    }
    if (myBorder != null) {
      Insets insets = myBorder.getBorderInsets(component.getComponent());
      myBorder.paintBorder(component.getComponent(), g, x, y, wholeWidth, wholeHeight);
      x += insets.left;
      y += insets.top;
      wholeWidth -= AwtUtil.getInsetWidth(insets);
      wholeHeight -= AwtUtil.getInsetHeight(insets);
    }
    if (myIcon != null) {
      int iconSpaceWidth = myIcon.getIconWidth() + getIconMarginWidth();
      if (myIconOpaque) {
        Color c = myDefaultAttributes.getBackground();
        if (c != null) {
          g.setColor(c);
          g.fillRect(x, y, iconSpaceWidth, wholeHeight);
        }
      }
      x += myIconMargin.left;
      wholeWidth -= myIconMargin.left;
      int height = myIcon.getIconHeight();
      int iconY = y + (wholeHeight - height) / 2;
      myIcon.paintIcon(component.getComponent(), g, x, iconY);
      x += iconSpaceWidth;
      wholeWidth -= iconSpaceWidth;
    }
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myLines.size(); i++) {
      SectionLine line = myLines.get(i);
      Graphics graphics = Env.isMac() ? g.create() : g;
      try {
        y += line.paint(x, y, graphics, wholeWidth, wholeHeight, component);
      } finally {
        if (graphics != g)
          graphics.dispose();
      }
    }
  }

  public void getSize(Dimension dimension, CanvasComponent component) {
    dimension.height = 0;
    dimension.width = 0;
    Dimension tempDimension = new Dimension();
    for (int i = 0; i < myLines.size(); i++) {
      SectionLine line = myLines.get(i);
      line.getSize(tempDimension, component);
      dimension.height += tempDimension.height;
      dimension.width = Math.max(dimension.width, tempDimension.width);
    }
    if (myIcon != null) {
      dimension.height = Math.max(myIcon.getIconHeight(), dimension.height);
      dimension.width += myIcon.getIconWidth();
      dimension.width += getIconMarginWidth();
    }
    if (myBorder != null) {
      Insets insets = myBorder.getBorderInsets(component.getComponent());
      AwtUtil.addInsets(dimension, insets);
    }
  }

  private int getIconMarginWidth() {
    return myIconMargin.left + myIconMargin.right;
  }

  public void updateComponent(CanvasComponent component) {
    if (myClearComponent) {
      component.clear();
      myClearComponent = false;
    }
    if (myComponentFont != null) {
      component.setComponentFont(myComponentFont);
      myComponentFont = null;
    }
  }

  public Line[] getLines() {
    return myLines.toArray(new SectionLine[myLines.size()]);
  }
}
