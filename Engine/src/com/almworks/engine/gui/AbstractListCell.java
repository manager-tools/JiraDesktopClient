package com.almworks.engine.gui;

import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class AbstractListCell implements TableRendererCell {
  private final ComponentProperty<Integer> CELL_INDEX = ComponentProperty.createProperty("valueListCell");
  protected final TableRenderer myRenderer;
  protected final TypedKey<List<TableRendererCell>> myCellsKey;

  public AbstractListCell(TableRenderer renderer, String keyName) {
    myRenderer = renderer;
    myCellsKey = TypedKey.create(keyName);
  }

  public int getHeight(RendererContext context) {
    List<TableRendererCell> cells = getCells(context);
    int height = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < cells.size(); i++) {
      TableRendererCell cell = cells.get(i);
      height += cell.getHeight(context);
    }
    if (!cells.isEmpty())
      height += myRenderer.getVerticalGap() * (cells.size() - 1);
    return height;
  }

  public int getWidth(RendererContext context) {
    int maxWidth = 0;
    List<TableRendererCell> cells = getCells(context);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < cells.size(); i++) {
      TableRendererCell cell = cells.get(i);
      maxWidth = Math.max(maxWidth, cell.getWidth(context));
    }
    return maxWidth;
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    List<TableRendererCell> cells = getCells(context);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < cells.size(); i++) {
      TableRendererCell cell = cells.get(i);
      cell.paint(g, x, y, context);
      y += cell.getHeight(context) + myRenderer.getVerticalGap();
    }
  }



  public void invalidateLayout(RendererContext context) {
  }

  

  @Nullable
  public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
    List<TableRendererCell> cells = getCells(context);
    int yOffset = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < cells.size(); i++) {
      if (y < 0)
        return null;
      TableRendererCell cell = cells.get(i);
      int height = cell.getHeight(context);
      if (y < height) {
        rectangle.y = yOffset;
        RendererActivity activity = cell.getActivityAt(id, x, y, context, rectangle);
        if (activity != null)
          activity.storeValue(CELL_INDEX, i);
        return activity;
      }
      int verticalStep = height + myRenderer.getVerticalGap();
      y -= verticalStep;
      yOffset += verticalStep;
    }
    return null;
  }

  @Nullable
  public JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next)
  {
    Integer firstCell;
    if (current != null)
      firstCell = CELL_INDEX.getClientValue(current);
    else
      firstCell = -1;
    if (firstCell == null) {
      assert false : current;
      firstCell = -1;
    }
    List<TableRendererCell> cells = getCells(context);
    if (firstCell == -1) {
      firstCell = next ? 0 : cells.size() - 1;
      current = null;
    }
    int savedX = targetArea.x;
    int savedWidth = targetArea.width;
    int savedHeight = targetArea.height;
    for (int i = firstCell; next ? (i < cells.size()) : (i >= 0); i += next ? 1 : -1) {
      targetArea.x = savedX;
      targetArea.y = 0;
      targetArea.width = savedWidth;
      targetArea.height = savedHeight;
      TableRendererCell cell = cells.get(i);
      JComponent result = cell.getNextLifeComponent(context, current, targetArea, next);
      if (result != null) {
        CELL_INDEX.putClientValue(result, i);
        targetArea.y += getCellTop(context, i);
        return result;
      }
      current = null;
    }
    return null;
  }

  private int getCellTop(RendererContext context, int index) {
    List<TableRendererCell> cells = getCells(context);
    assert index >= 0 && index < cells.size();
    int result = 0;
    for (int i = 0; i < index; i++)
      result += cells.get(i).getHeight(context) + myRenderer.getVerticalGap();
    return result;
  }

  private List<TableRendererCell> getCells(RendererContext context) {
    List<TableRendererCell> cells = context.getCachedValue(myCellsKey);
    if (cells == null) {
      cells = createCells(context);
      context.cacheValue(myCellsKey, cells);
    }
    return cells;
  }

  protected abstract List<TableRendererCell> createCells(RendererContext context);
}
