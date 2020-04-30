package com.almworks.util.components.renderer.table;

import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public interface TableRendererLine {
  int getHeight(int width, RendererContext context);

  int getWidth(int columnIndex, RendererContext context);

  boolean isVisible(RendererContext context);

  void paint(Graphics g, int y, RendererContext context);

  void invalidateLayout(RendererContext context);

  @Nullable
  RendererActivity getActivityAt(int id, int columnIndex, int x, int y, RendererContext context, Rectangle rectangle);

  int getPreferedWidth(int column, RendererContext context);

  @Nullable
  JComponent getNextLiveComponent(@NotNull RendererContext context, TableRenderer tableRenderer,
    @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next);

  boolean getLineRectangle(int y, Rectangle lineRectangle, RendererContext context);
}
