package com.almworks.util.components.renderer;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author dyoma
 */
public interface TextAttributes {
  int NO_STYLE = -1;

  Color getForeground();

  /**
   * @return background color, null means don't fillRect
   */
  @Nullable
  Color getBackground();

  int getFontStyle();  
}
