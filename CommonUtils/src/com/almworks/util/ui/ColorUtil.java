package com.almworks.util.ui;

import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Contract;

import java.awt.*;

public class ColorUtil {
  private static final float BRED = 0.213F;
  private static final float BGREEN = 0.715F;
  private static final float BBLUE = 0.072F;

  public static Color transform(Color color, int tuneRed, int tuneGreen, int tuneBlue, int tuneBright) {
    float r = floatify(color.getRed());
    float g = floatify(color.getGreen());
    float b = floatify(color.getBlue());
    float y = getBrightness(r, g, b);

    float nr = r * coef(tuneRed);
    float ng = g * coef(tuneGreen);
    float nb = b * coef(tuneBlue);
    float ny = y * coef(tuneBright);
    ny = Math.min(Math.max(ny, 0F), 1F);

    float ry = getBrightness(nr, ng, nb);
    float diff = ry - ny;

    nr = Math.min(Math.max(nr - diff * BRED, 0), 1);
    ng = Math.min(Math.max(ng - diff * BGREEN, 0), 1);
    nb = Math.min(Math.max(nb - diff * BBLUE, 0), 1);

    assert Math.abs(getBrightness(nr, ng, nb) - ny) < 0.01;
    return new Color(nr, ng, nb);
  }

  private static float coef(int tune) {
    if (tune == 0)
      return 1F;
    if (tune < 0) {
      if (tune < -9)
        tune = -9;
      return (float) (10 + tune) / 10F;
    } else {
      if (tune > 9)
        tune = 9;
      return 10F / (float) (10 - tune);
    }
  }

  private static float floatify(int v) {
    if (v < 0)
      v = 0;
    else if (v > 255)
      v = 255;
    return ((float) v + 1F) / 256F;
  }

  private static float getBrightness(float r, float g, float b) {
    return r * BRED + g * BGREEN + b * BBLUE;
  }

  public static Color adjustHSB(Color color, float hue, float saturation, float brightness) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    if (hue >= 0)
      hsb[0] = hue;
    if (saturation >= 0)
      hsb[1] = saturation;
    if (brightness >= 0)
      hsb[2] = brightness;
    color = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    return color;
  }

  public static String formatColor(Color color) {
    final StringBuffer formatted = new StringBuffer(Integer.toHexString(color.getRGB()));
    while (formatted.length() < 8)
      formatted.insert(0, '0');
    return '#' + Util.upper(formatted.substring(2));
  }

  @Contract("!null,!null,_ -> !null")
  public static Color between(Color from, Color to, float percent) {
    if (to == null && from == null) {
      assert false;
      return null;
    }
    if (to == null) {
      assert false;
      Log.debug("to null", new NullPointerException());
      return from;
    }
    if (from == null) {
      assert false;
      Log.debug("from null", new NullPointerException());
      return to;
    }
    if (percent < 0)
      percent = 0;
    if (percent > 1)
      percent = 1;
    int red = normalize(from.getRed() + Math.round(percent * (to.getRed() - from.getRed())));
    int green = normalize(from.getGreen() + Math.round(percent * (to.getGreen() - from.getGreen())));
    int blue = normalize(from.getBlue() + Math.round(percent * (to.getBlue() - from.getBlue())));
    return new Color(red, green, blue);
  }

  private static int normalize(int colorComponent) {
    if (colorComponent < 0)
      colorComponent = 0;
    if (colorComponent > 255)
      colorComponent = 255;
    return colorComponent;
  }

  public static Color getStripeBackground(Color background) {
    return between(background, Color.GRAY, 0.1F);
  }
}
