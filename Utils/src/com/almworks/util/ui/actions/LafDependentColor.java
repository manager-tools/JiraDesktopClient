package com.almworks.util.ui.actions;

import javax.swing.*;
import java.awt.*;

/**
 * An instance of this class incapsulates a {@code Color} that
 * depends on a certain system color (obtained from {@link UIManager}).
 * When that color changes, the incapsulated color is recreated.
 */
public abstract class LafDependentColor {
  private final String myLafColorName;
  private int myLastLafRGB;
  private Color myCurrentColor;

  /**
   * The constructor.
   * @param lafColorName The name of the base system color.
   */
  public LafDependentColor(String lafColorName) {
    myLafColorName = lafColorName;
  }

  /**
   * Override this method to transform the system color into
   * the dependent color. The default implementation returns
   * the system color unchanged.
   * @param lafColor The system color.
   * @return The transformed (dependent) color.
   */
  public Color makeColor(Color lafColor) {
    return lafColor;
  }

  /**
   * Returns the dependent color. Checks the system color and
   * reconstructs the dependent color if necessary.
   * @return The dependent color.
   */
  public Color getColor() {
    final Color lafColor = UIManager.getColor(myLafColorName);
    if(lafColor == null) {
      return null;
    }

    final int lafRGB = lafColor.getRGB();
    if(myCurrentColor == null || myLastLafRGB != lafRGB) {
      myLastLafRGB = lafRGB;
      myCurrentColor = makeColor(lafColor);
    }

    return myCurrentColor;
  }
}
