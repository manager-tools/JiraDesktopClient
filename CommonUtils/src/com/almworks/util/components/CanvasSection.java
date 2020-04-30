package com.almworks.util.components;

import javax.swing.border.Border;
import java.awt.*;

/**
 * @author dyoma
 */
public interface CanvasSection {
  CanvasSection appendText(String text);

  void setForeground(Color foreground);

  void setBackground(Color background);

  void setBorder(Border border);

  CanvasSection setFontStyle(int style);

  CanvasSection appendInt(int value);

  CanvasSection appendLong(long value);

  boolean isEmpty();

  String getText();

  void copyAttributes(CanvasSection section);
}
