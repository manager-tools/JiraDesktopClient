package com.almworks.util.components.renderer;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author dyoma
 */
public class RootAttributes implements TextAttributes {
  public Color myForeground = null;

  @Nullable
  public Color myBackground = null;
  public int myFontStyle = NO_STYLE;
  public boolean myEnabled = true;

  public void clear() {
    myForeground = null;
    myBackground = null;
    myFontStyle = NO_STYLE;
    myEnabled = true;
  }

  public void copyTo(com.almworks.util.components.Canvas canvas) {
    canvas.setForeground(myForeground);
    canvas.setBackground(myBackground);
    canvas.setFontStyle(myFontStyle);
    canvas.setEnabled(myEnabled);
  }

  public Color getForeground() {
    return myForeground;
  }

  public Color getBackground() {
    return myBackground;
  }

  public int getFontStyle() {
    return myFontStyle;
  }  
}
