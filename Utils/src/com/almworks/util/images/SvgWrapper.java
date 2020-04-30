package com.almworks.util.images;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Convenient class to adapt SVG graphics to Swing interfaces.<br>
 * Adapts SVG to:
 * <ul>
 *   <li>{@link Image}: {@link #renderImage(int, int)}</li>
 *   <li>{@link Icon}: {@link #createImageIcon()}</li>
 * </ul><br>
 * Usage:<br>
 * 1. Load SVG with one of constructor methods. Supports load from: {@link #load(File) file}, {@link #loadBytes(String, byte[]) raw byte array}<br>
 * 2. Convert loaded SVG to Swing interface (such as Image or Icon). You may specify desired size to override default.<br><br>
 * Example:<br>
 * <pre>
 *   SvgWrapper svg = SvgWrapper.load(file);       // Load SVG from file
 *   ImageIcon icon = svg.createImageIcon(20, 20); // Convert to a 20x20 pixels icon
 * </pre>
 */
public class SvgWrapper {
  public static final String DEFAULT_MIME_TYPE = "image/svg+xml";;
  private final SVGDiagram myDiagram;

  public SvgWrapper(SVGDiagram diagram) {
    myDiagram = diagram;
  }

  public static SvgWrapper load(File file) throws IOException {
    SVGUniverse universe = new SVGUniverse();
    URI uri = universe.loadSVG(file.toURI().toURL());
    return create(universe, uri, file);
  }

  @NotNull
  private static SvgWrapper create(SVGUniverse universe, URI uri, Object debugSource) throws IOException {
    SVGDiagram diagram = universe.getDiagram(uri);
    if (diagram == null) throw new IOException("Failed to load SVG file: " + debugSource);
    return new SvgWrapper(diagram);
  }

  @NotNull
  public static SvgWrapper loadBytes(String name, byte[] bytes) throws IOException {
    SVGUniverse universe = new SVGUniverse();
    URI uri = universe.loadSVG(new ByteArrayInputStream(bytes), name);
    return create(universe, uri, "rawBytes:" + name);
  }

  @NotNull
  public ImageIcon createImageIcon() throws IOException {
    return new ImageIcon(renderImage());
  }

  @NotNull
  public ImageIcon createImageIcon(int width, int height) throws IOException {
    return new ImageIcon(renderImage(width, height));
  }

  /**
   * Renders SVG to an image of specified size
   * @param width desired image width
   * @param height desired image height
   * @return rendered SVG
   * @throws IOException
   */
  public BufferedImage renderImage(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D graphics = image.createGraphics();
    Rectangle2D.Double rect = new Rectangle2D.Double();
    myDiagram.getViewRect(rect);
    AffineTransform scale = new AffineTransform();
    scale.setToScale((double)width / rect.width, (double)height / rect.height);
    graphics.transform(scale);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    try {
      myDiagram.render(graphics);
    } catch (SVGException e) {
      throw new IOException(e);
    }
    graphics.dispose();
    return image;
  }

  public Dimension fitRectangle(float width, float height) {
    if (width < 1 || height < 1) return new Dimension(1, 1);
    Rectangle2D rect = myDiagram.getViewRect();
    float diagramRatio = (float) (rect.getWidth() / rect.getHeight());
    double targetRatio = width / height;
    if (diagramRatio < targetRatio) return new Dimension(Math.round(height * diagramRatio), Math.round(height));
    else return new Dimension(Math.round(width), Math.round(width / diagramRatio));
  }

  /**
   * Renders SVG to an image of default size
   * @return rendered SVG
   * @throws SVGException
   * @see #createImageIcon(int, int)
   */
  @NotNull
  public BufferedImage renderImage() throws IOException {
    return renderImage((int) Math.ceil(myDiagram.getWidth()), (int) Math.ceil(myDiagram.getHeight()));
  }

  public static boolean isSVG(String mimeType) {
    return mimeType != null && Util.lower(mimeType).contains("image/svg");
  }
}
