package com.almworks.util.components.renderer;

import com.almworks.util.components.CanvasSection;

import javax.swing.border.Border;
import java.awt.*;

/**
 * @author dyoma
 */
class InheritedAttributes implements TextAttributes {
  private final TextAttributes myParent;
  public Color myForeground = null;
  public Color myBackground = null;
  public int myFontStyle = NO_STYLE;
  public Border myBorder = null;
  public boolean myEnabled = true;

  public InheritedAttributes(TextAttributes parent) {
    myParent = parent;
  }

  public Color getForeground() {
    return myForeground != null ? myForeground : myParent.getForeground();
  }

  public Color getBackground() {
    return myBackground != null ? myBackground : myParent.getBackground();
  }

  public Border getBorder() {
    return myBorder;
  }

  public int getFontStyle() {
    return myFontStyle != NO_STYLE ? myFontStyle : myParent.getFontStyle();
  }

  public void clear() {
    myForeground = null;
    myBackground = null;
    myFontStyle = NO_STYLE;
    myBorder = null;
    myEnabled = true;
  }

  public boolean isEmpty() {
    return myForeground == null && myBackground == null && myFontStyle == NO_STYLE && myBorder == null && myEnabled;
  }

  public void copyTo(CanvasSection section) {
    section.setForeground(myForeground);
    section.setBackground(myBackground);
    section.setFontStyle(myFontStyle);
    section.setBorder(myBorder);
  }
}
