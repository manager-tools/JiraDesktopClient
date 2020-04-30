package com.almworks.util.components;

import com.almworks.util.Env;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * @author dyoma
 */
public abstract class DelegatingGraphics extends Graphics2D {
  private final Graphics2D myGraphics;

  protected DelegatingGraphics(Graphics2D graphics) {
    // must not use graphics decorators on mac
    assert !Env.isMac();
    myGraphics = graphics;
  }

  public Graphics create() {
    return wrap((Graphics2D) myGraphics.create());
  }

  protected abstract DelegatingGraphics wrap(Graphics2D graphics2D);

  protected final Graphics2D getGraphics() {
    return myGraphics;
  }

  public void rotate(double theta) {
    myGraphics.rotate(theta);
  }

  public void scale(double sx, double sy) {
    myGraphics.scale(sx, sy);
  }

  public void shear(double shx, double shy) {
    myGraphics.shear(shx, shy);
  }

  public void translate(double tx, double ty) {
    myGraphics.translate(tx, ty);
  }

  public void rotate(double theta, double x, double y) {
    myGraphics.rotate(theta, x, y);
  }

  public void translate(int x, int y) {
    myGraphics.translate(x, y);
  }

  public Color getBackground() {
    return myGraphics.getBackground();
  }

  public void setBackground(Color color) {
    myGraphics.setBackground(color);
  }

  public Composite getComposite() {
    return myGraphics.getComposite();
  }

  public void setComposite(Composite comp) {
    myGraphics.setComposite(comp);
  }

  public GraphicsConfiguration getDeviceConfiguration() {
    return myGraphics.getDeviceConfiguration();
  }

  public Paint getPaint() {
    return myGraphics.getPaint();
  }

  public void setPaint(Paint paint) {
    myGraphics.setPaint(paint);
  }

  public RenderingHints getRenderingHints() {
    return myGraphics.getRenderingHints();
  }

  public void clip(Shape s) {
    myGraphics.clip(s);
  }

  public void draw(Shape s) {
    myGraphics.draw(s);
  }

  public void fill(Shape s) {
    myGraphics.fill(s);
  }

  public Stroke getStroke() {
    return myGraphics.getStroke();
  }

  public void setStroke(Stroke s) {
    myGraphics.setStroke(s);
  }

  public FontRenderContext getFontRenderContext() {
    return myGraphics.getFontRenderContext();
  }

  public void drawGlyphVector(GlyphVector g, float x, float y) {
    myGraphics.drawGlyphVector(g, x, y);
  }

  public AffineTransform getTransform() {
    return myGraphics.getTransform();
  }

  public void setTransform(AffineTransform Tx) {
    myGraphics.setTransform(Tx);
  }

  public void transform(AffineTransform Tx) {
    myGraphics.transform(Tx);
  }

  public void drawString(String s, float x, float y) {
    myGraphics.drawString(s, x, y);
  }

  public void drawString(String str, int x, int y) {
    myGraphics.drawString(str, x, y);
  }

  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    myGraphics.drawString(iterator, x, y);
  }

  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    myGraphics.drawString(iterator, x, y);
  }

  public void addRenderingHints(Map hints) {
    myGraphics.addRenderingHints(hints);
  }

  public void setRenderingHints(Map hints) {
    myGraphics.setRenderingHints(hints);
  }

  public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
    return myGraphics.hit(rect, s, onStroke);
  }

  public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    myGraphics.drawRenderedImage(img, xform);
  }

  public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    myGraphics.drawRenderableImage(img, xform);
  }

  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    myGraphics.drawImage(img, op, x, y);
  }

  public Object getRenderingHint(RenderingHints.Key hintKey) {
    return myGraphics.getRenderingHint(hintKey);
  }

  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
    myGraphics.setRenderingHint(hintKey, hintValue);
  }

  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
    return myGraphics.drawImage(img, xform, obs);
  }

  public void dispose() {
    myGraphics.dispose();
  }

  public void setPaintMode() {
    myGraphics.setPaintMode();
  }

  public void clearRect(int x, int y, int width, int height) {
    myGraphics.clearRect(x, y, width, height);
  }

  public void clipRect(int x, int y, int width, int height) {
    myGraphics.clipRect(x, y, width, height);
  }

  public void drawLine(int x1, int y1, int x2, int y2) {
    myGraphics.drawLine(x1, y1, x2, y2);
  }

  public void drawOval(int x, int y, int width, int height) {
    myGraphics.drawOval(x, y, width, height);
  }

  public void fillOval(int x, int y, int width, int height) {
    myGraphics.fillOval(x, y, width, height);
  }

  public void fillRect(int x, int y, int width, int height) {
    myGraphics.fillRect(x, y, width, height);
  }

  public void setClip(int x, int y, int width, int height) {
    myGraphics.setClip(x, y, width, height);
  }

  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    myGraphics.copyArea(x, y, width, height, dx, dy);
  }

  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    myGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
  }

  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    myGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    myGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
  }

  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    myGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    myGraphics.drawPolygon(xPoints, yPoints, nPoints);
  }

  public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    myGraphics.drawPolyline(xPoints, yPoints, nPoints);
  }

  public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    myGraphics.fillPolygon(xPoints, yPoints, nPoints);
  }

  public Color getColor() {
    return myGraphics.getColor();
  }

  public void setColor(Color c) {
    myGraphics.setColor(c);
  }

  public void setXORMode(Color c1) {
    myGraphics.setXORMode(c1);
  }

  public Font getFont() {
    return myGraphics.getFont();
  }

  public void setFont(Font font) {
    myGraphics.setFont(font);
  }

  public Rectangle getClipBounds() {
    return myGraphics.getClipBounds();
  }

  public Shape getClip() {
    return myGraphics.getClip();
  }

  public void setClip(Shape clip) {
    myGraphics.setClip(clip);
  }

  public String toString() {
    return "Delegating[" + myGraphics.toString() + "]";
  }

  public FontMetrics getFontMetrics(Font f) {
    return myGraphics.getFontMetrics(f);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
    ImageObserver observer) {
    return myGraphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
    return myGraphics.drawImage(img, x, y, width, height, observer);
  }

  public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
    return myGraphics.drawImage(img, x, y, observer);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
    Color bgcolor, ImageObserver observer) {
    return myGraphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
    return myGraphics.drawImage(img, x, y, width, height, bgcolor, observer);
  }

  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
    return myGraphics.drawImage(img, x, y, bgcolor, observer);
  }

/*
  public void draw3DRect(int x, int y, int width, int height, boolean raised) {
    myGraphics.draw3DRect(x, y, width, height, raised);
  }

  public void fill3DRect(int x, int y, int width, int height, boolean raised) {
    myGraphics.fill3DRect(x, y, width, height, raised);
  }

  public Graphics create(int x, int y, int width, int height) {
    return wrap((Graphics2D) myGraphics.create(x, y, width, height));
  }

  public FontMetrics getFontMetrics() {
    return myGraphics.getFontMetrics();
  }

  public void drawRect(int x, int y, int width, int height) {
    myGraphics.drawRect(x, y, width, height);
  }

  public void drawPolygon(Polygon p) {
    myGraphics.drawPolygon(p);
  }

  public void fillPolygon(Polygon p) {
    myGraphics.fillPolygon(p);
  }

  public void drawChars(char data[], int offset, int length, int x, int y) {
    myGraphics.drawChars(data, offset, length, x, y);
  }

  public void drawBytes(byte data[], int offset, int length, int x, int y) {
    myGraphics.drawBytes(data, offset, length, x, y);
  }

  public Rectangle getClipRect() {
    return myGraphics.getClipRect();
  }

  public boolean hitClip(int x, int y, int width, int height) {
    return myGraphics.hitClip(x, y, width, height);
  }

  public Rectangle getClipBounds(Rectangle r) {
    return myGraphics.getClipBounds(r);
  }

*/

}
