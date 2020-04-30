package com.almworks.engine.gui;

import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SeparatorLine implements TableRendererLine {
  public final int myGap;

  public SeparatorLine(int gap) {
    myGap = gap;
  }

  public int getHeight(int width, RendererContext context) {
    return myGap;
  }

  public int getWidth(int columnIndex, RendererContext context) {
    return 0;
  }

  public boolean isVisible(RendererContext context) {
    return true;
  }

  public void paint(Graphics g, int y, RendererContext context) {
  }

  public void invalidateLayout(RendererContext context) {
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int columnIndex, int x, int y, RendererContext context,
    Rectangle rectangle)
  {
    return null;
  }

  public int getPreferedWidth(int column, RendererContext context) {
    return 0;
  }

  @Nullable
  public JComponent getNextLiveComponent(@NotNull RendererContext context, TableRenderer tableRenderer,
    @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next)
  {
    return null;
  }

  public boolean getLineRectangle(int y, Rectangle lineRectangle, RendererContext context) {
    return false;
  }
}
