package com.almworks.screenshot.editor.tools.annotate;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.layers.LayerImageControl;
import com.almworks.screenshot.editor.shapes.DoubleRectShape;
import com.almworks.screenshot.editor.tools.highlight.HighlightShape;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/**
 * @author Stalex
 */
public class AnnotationShape extends DoubleRectShape {

  private final static int MIN_WIDTH = 100;

  private final static int MIN_HEIGHT = 50;

  private static final int ROUNDING = 10;

  public String myText = "";

  private Font myFont = null;

  private Color myFontColor = null;

  private Color myLineColor;

  private Color myFillColor;

  private boolean isFirstEdit = true;

  private final boolean myIsSingleText;

  private JTextArea myTextDrawer = new JTextArea();

  private void initTextDrawer() {
    myTextDrawer.setLineWrap(true);
    myTextDrawer.setWrapStyleWord(true);
    myTextDrawer.setOpaque(false);
  }

  public AnnotationShape(LayerImageControl imageControl, Rectangle rect, Rectangle linkedRect, Font currentFont, Color color, Color currentFontColor, boolean isSingleText) {
    super(imageControl, color);

    myIsSingleText = isSingleText;
    myFont = currentFont;
    myFontColor = currentFontColor;

    initTextDrawer();
    setColor(color);

    setRect(rect);
    setLinkedRect(linkedRect);
    adjustLinkedRect();
  }

  public AnnotationShape(LayerImageControl imageControl, Rectangle rect, Font currentFont, Color color, Color currentFontColor) {
    this(imageControl, rect, findLinkedPlace(rect, imageControl.getBounds(), MIN_WIDTH, MIN_HEIGHT), currentFont, color, currentFontColor, false);
  }

  public AnnotationShape(LayerImageControl imageControl, Point location, Font currentFont, Color color, Color currentFontColor) {
    this(imageControl, new Rectangle(), new Rectangle(location.x, location.y, MIN_WIDTH, MIN_HEIGHT), currentFont, color, currentFontColor, true);
  }

  @Override
  public boolean contains(Point point) {
    if (isSingleText()) {
      return getLinkedRect().contains(point);
    } else {
      return super.contains(point);
    }
  }

  @Override
  public boolean intersects(Rectangle p) {
    if (isSingleText()) {
      return getLinkedRect().intersects(p);
    } else {
      return super.intersects(p);
    }
  }

  @Override
  public void setColor(Color myColor) {
    super.setColor(myColor);

    myLineColor = new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 170);
    myFillColor = new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 50);
  }

  @Override
  public Rectangle getBounds() {
    Rectangle rectangle = getLinkedRect().getBounds();
    if (!isSingleText()) {
      rectangle = rectangle.union(getRect());
    }

    int growParam = HighlightShape.STROKE_WIDTH + 1;
    rectangle.grow(growParam, growParam);
    return rectangle;
  }

  public boolean isFirstEdit() {
    return isFirstEdit;
  }


  public String getText() {
    return myText;
  }


  public Font getFont() {
    return myFont == null ? myTextDrawer.getFont() : myFont;
  }

  public Color getFontColor() {
    return myFontColor == null ? myTextDrawer.getForeground() : myFontColor;
  }

  private void adjustLinkedRect() {
    myTextDrawer.setText(myText);
    if (myFont != null) {
      myTextDrawer.setFont(myFont);
    }
    myTextDrawer.setBounds(getTextBounds());
    setTextBounds(myTextDrawer.getPreferredSize());
  }

  public void setText(String text) {
    myText = text;
    isFirstEdit = false;
    adjustLinkedRect();

  }

  public void setFontAndColor(Font font, Color fontColor) {
    myFont = font;
    myFontColor = fontColor;
    adjustLinkedRect();
  }

  @Override
  public void setLinkedRect(Rectangle runningSelection) {
    super.setLinkedRect(runningSelection);
    adjustLinkedRect();
  }

  public Rectangle getTextBounds() {
    Rectangle r = new Rectangle(getLinkedRect());
    r.setSize(r.width - 2 * ROUNDING, r.height - 2 * ROUNDING);
    r.setLocation(r.x + ROUNDING, r.y + ROUNDING);
    return r;
  }

  public void setTextBounds(Dimension textRect) {
    Rectangle newRect = getLinkedRect();
    newRect.setSize(textRect.width + 2 * ROUNDING, textRect.height + 2 * ROUNDING);
    super.setLinkedRect(newRect);  
  }

  @Override
  public void paintShape(Graphics2D g2, Area clip) {
    AwtUtil.applyRenderingHints(g2);
    if (!isSingleText()) {
      ImageEditorUtils.drawRoundRect(g2, getRect(), myLineColor, HighlightShape.NOTE_AREA_STROKE, ImageEditorUtils.getRounding(getRect()));
      paintLink(g2, myLineColor);
    }
    paintNoteArea(getLinkedRect(), g2, true);
    paintText(g2);
  }

  protected void paintLink(Graphics2D g2, Color color) {

    Area origClip = new Area(g2.getClip());
    Area subClip = new Area(origClip);

    subClip.subtract(new Area(getRoundArea(getRect(),ImageEditorUtils.getRounding(getRect()))));
    subClip.subtract(new Area(getRoundArea(getLinkedRect(),ROUNDING)));

    g2.setClip(subClip);

    super.paintLink(g2, color);

    g2.setClip(origClip);
  }

  private void paintText(Graphics2D gd) {
    Object globalAntialiasing = gd.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    try {
      // turn off Java antialiasing
      gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      Rectangle textBounds = getTextBounds();
      myTextDrawer.setBounds(textBounds);
      assert (myFontColor != null);
      myTextDrawer.setForeground(myFontColor);
      myTextDrawer.setText(myText);
      if (myFont != null) {
        myTextDrawer.setFont(myFont);
      }

      gd.translate(textBounds.getX(), textBounds.getY());
      myTextDrawer.paint(gd);
      gd.translate(-textBounds.getX(), -textBounds.getY());
    } finally {
      if (globalAntialiasing != null) {
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, globalAntialiasing);
      }
    }
  }

  private static RoundRectangle2D getRoundArea(Rectangle rectangle, int rounding) {
    double leave = -HighlightShape.STROKE_WIDTH / 2;
    return new RoundRectangle2D.Double(rectangle.x - leave, rectangle.y - leave, rectangle.width + 2 * leave, rectangle.height + 2 * leave, rounding, rounding);
  }

  private void paintNoteArea(Rectangle selection, Graphics2D g2, boolean fill) {

    RoundRectangle2D area = getRoundArea(selection, ROUNDING);
    g2.setStroke(HighlightShape.NOTE_AREA_STROKE);
    g2.setColor(myLineColor);
    g2.draw(area);

    if (fill) {
      g2.setColor(myFillColor);
      g2.fill(area);
    }
  }

  
  public boolean isSingleText() {
    return myIsSingleText;
  }
}
