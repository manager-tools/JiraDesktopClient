package com.almworks.util.images;

import com.almworks.util.LogHelper;
import com.almworks.util.bmp.BMPException;
import com.almworks.util.bmp.BMPReader;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.Computable;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtil {
  public static Image createStubImage(int imageID, int assumedWidth, int assumedHeight) {
//    int size = Math.max(assumedWidth, assumedHeight);
    String text = createText(imageID);

    BufferedImage image = new BufferedImage(assumedWidth, assumedHeight, BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = (Graphics2D) image.getGraphics();
    g.setColor(new Color(30, 100, 196));
    int fontSize = (int) Math.round(9.0 * assumedHeight / 16.0);
    Font f = Font.decode("Tahoma-BOLD-" + fontSize);
    g.setFont(f);
    boolean smooth = fontSize > 12;
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      smooth ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
      smooth ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    LineMetrics lineMetrics = f.getLineMetrics(text, g.getFontRenderContext());
    Rectangle stringBounds = f.getStringBounds(text, g.getFontRenderContext()).getBounds();

    int height = (int) Math.ceil(lineMetrics.getAscent()) + 1;
    int width = stringBounds.width;
    int x = (assumedWidth - width) / 2;
    if (x < 0)
      x = 0;
    int y = assumedHeight - 3;
/*
    if (y < 0)
      y = 0;
*/

    g.drawString(text, x, y);
    g.dispose();

    return image;
  }

  private static String createText(int imageID) {
    StringBuffer buffer = new StringBuffer(".");
    if (imageID < 10)
      buffer.append('0');
    buffer.append(imageID);
    return buffer.toString();
  }

  public static Image createGrayscaleImage(Image image) {
/*
    BufferedImage source = createBufferedImage(image);
    BufferedImageOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
    BufferedImage result = op.filter(source, null);
    return result;
*/
/*
    return GrayFilter.createDisabledImage(image);
*/
    Color background = AwtUtil.getPanelBackground();
    return createSingleHueImage(image, background, null);
  }

  public static Image createSingleHueImage(Image image, Color sample, final Integer keepColor) {
    final float[] hsb = Color.RGBtoHSB(sample.getRed(), sample.getGreen(), sample.getBlue(), null);
    final float midB = hsb[2];
    final float highB = midB * 2 >= 1 ? 1F : midB * 2;
    final float lowB = midB / 2;

    RGBImageFilter filter = new RGBImageFilter() {
      public int filterRGB(int x, int y, int rgb) {
        if (keepColor != null && rgb == keepColor)
          return rgb;
        int gray = (int) ((0.30 * ((rgb >> 16) & 0xff) + 0.59 * ((rgb >> 8) & 0xff) + 0.11 * (rgb & 0xff)) / 3);
        float brightness = (float) gray / 255;
        if (brightness >= 0.5F)
          brightness = midB + (brightness - 0.5F) * 2F * (highB - midB);
        else
          brightness = midB - (0.5F - brightness) * 2F * (midB - lowB);
        brightness = Math.max(Math.min(brightness, highB), lowB);
        hsb[2] = brightness;
        int result = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return (rgb & 0xff000000) | (result & 0x00ffffff);
      }
    };

    ImageProducer prod = new FilteredImageSource(image.getSource(), filter);
    return Toolkit.getDefaultToolkit().createImage(prod);
  }

  public static BufferedImage createBufferedImage(Image image) {
    return createBufferedImage(image, Transparency.TRANSLUCENT);
  }

  public static BufferedImage createBufferedImage(Image image, int transparency) {
    // original idea from http://javaalmanac.com/egs/java.awt.image/Image2Buf.html?l=rel

    // This code ensures that all the pixels in the image are loaded
    image = new ImageIcon(image).getImage();

    // Create a buffered image with a format that's compatible with the screen
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = env.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
    BufferedImage buffer = config.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
    if (buffer == null) {
      // Create a buffered image using the default color model
      buffer = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    }
    Graphics g = buffer.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return buffer;
  }

  @Nullable
  @CanBlock
  public static Image loadImageFromFile(@NotNull final File file, @Nullable final String mimeType) {
    if (SvgWrapper.isSVG(mimeType)) return loadSvgImage(file);
    final byte[] bytes;
    try {
      bytes = FileUtil.loadFile(file);
    } catch (IOException e) {
      Log.debug("cannot read " + file, e);
      return null;
    }
    Image image = ThreadGate.AWT_IMMEDIATE.compute(new Computable<Image>() {
      public Image compute() {
        Image image = null;
        if (bytes != null) {
          if (isBitmap(file, mimeType)) {
            try {
              image = BMPReader.loadBMP(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
              // ignore
            } catch (BMPException e) {
              Log.warn("bad bitmap " + file, e);
            }
          } else {
            image = Toolkit.getDefaultToolkit().createImage(bytes);
          }
        }
        return image;
      }
    });
    return image;
  }

  @Nullable
  public static Image loadSvgImage(File file) {
    try {
      return SvgWrapper.load(file).renderImage();
    } catch (IOException e) {
      LogHelper.debug("Failed to load SVG", e);
      return null;
    }
  }

  public static boolean isBitmap(File file, String mimeType) {
    return (mimeType != null && mimeType.equalsIgnoreCase("image/bmp")) ||
      (mimeType == null && Util.lower(file.getName()).endsWith(".bmp"));
  }

  public static boolean isImageMimeType(String mimeType) {
    return mimeType != null && Util.lower(mimeType).startsWith("image/");
  }
}
