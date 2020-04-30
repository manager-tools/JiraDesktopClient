package com.almworks.util.ui.widgets;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Context to perform paint. Widgets should use provided methods, don't use graphics directly.
 */
public interface GraphContext extends CellContext {
  void drawRect(Rectangle rect);

  void fillRect(Rectangle rect);

  void fillRect(int x, int y, int width, int height);

  void drawRect(int x, int y, int width, int height);

  void setColor(Color color);

  FontMetrics getFontMetrics();

  void drawString(int x, int y, String text);

  /**
   * Same as {@link #drawString(int, int, String)} but trancates text and adds dots if text doesnt fit into cell bounds
   */
  void drawTrancatableString(int x, int y, String text);

  /**
   * Gives direct access to graphics. Direct access degradates performance. Extending the interface with new methods is prefered.
   */
  Graphics2D getGraphics();

  /**
   * @return clipping rectange in coordinates of current cell (cell's upper left corner in (0,0))
   */
  Rectangle getLocalClip(@Nullable Rectangle target);

  void setFontStyle(int style);
}
