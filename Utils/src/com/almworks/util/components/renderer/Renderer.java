package com.almworks.util.components.renderer;

import com.almworks.util.components.RendererActivityController;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public interface Renderer {
  int getPreferedWidth(RendererContext context);

  /**
   * This method is needed for WidthDrivenColumn, and is not used/implemented yet.
   */
  int getPreferedHeight(int width, RendererContext context);

  Dimension getPreferredSize(RendererContext context);

  void paint(Graphics g, RendererContext context);

  boolean isFocusable();

  /**
   *
   * @param id mouse event id
   * @param x mouse event x coordinate
   * @param y mouse event y coordinate
   * @param context
   * @param controller
   * @return true iff event is processed and should be consumed
   */
  boolean updateMouseActivity(int id, int x, int y, RendererContext context, RendererActivityController controller);

  void processMouseEvent(int id, int x, int y, RendererContext context, RendererActivityController controller);

  @Nullable
  JComponent getNextLiveComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next);

  void addAWTListener(Lifespan life, Listener listener);

  interface Listener {
    void onMouseOverRectangle(@Nullable Rectangle rect);
  }
}
