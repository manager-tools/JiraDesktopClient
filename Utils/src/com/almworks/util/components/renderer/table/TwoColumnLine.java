package com.almworks.util.components.renderer.table;

import com.almworks.util.Env;
import com.almworks.util.commons.Function;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.ui.ComponentProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TwoColumnLine implements TableRendererLine {
  private static final boolean isMac = Env.isMac();

  private final ComponentProperty<Integer> COLUMN = ComponentProperty.createProperty("column");
  private final TableRendererCell myFirst;
  private final TableRendererCell mySecond;
  private final TableRenderer myTable;

  private Function<RendererContext, Boolean> myVisibility;

  public TwoColumnLine(TableRendererCell first, TableRendererCell second, TableRenderer table) {
    myFirst = first;
    mySecond = second;
    myTable = table;
  }

  public int getHeight(int width, RendererContext context) {
    return Math.max(myFirst.getHeight(context), mySecond.getHeight(context));
  }

  public int getWidth(int columnIndex, RendererContext context) {
    assert columnIndex >= 0 : columnIndex;
    if (columnIndex >= 2 || columnIndex < 0)
      return 0;
    return columnIndex == 0 ? myFirst.getWidth(context) : mySecond.getWidth(context);
  }

  public boolean isVisible(RendererContext context) {
    Function<RendererContext, Boolean> visibility = myVisibility;
    if (visibility == null) {
      return true;
    }
    Boolean result = visibility.invoke(context);
    return result != null && result;
  }

  public TwoColumnLine setVisibility(Function<RendererContext, Boolean> visibility) {
    myVisibility = visibility;
    return this;
  }

  public void paint(Graphics g, int y, RendererContext context) {
    // Simulating right label alignment on the Mac; negative x is a hint for TextCell.
    // todo: find a less kludgy solution.
    final int firstX = (isMac && myFirst instanceof TextCell)
        ? -(myTable.getColumnWidth(0, context) + 8) : 0;
    myFirst.paint(g, firstX, y, context);
    mySecond.paint(g, myTable.getColumnX(1, context), y, context);
  }

  public void invalidateLayout(RendererContext context) {
    myFirst.invalidateLayout(context);
    mySecond.invalidateLayout(context);
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int columnIndex, int x, int y, RendererContext context,
    Rectangle rectangle)
  {
    assert columnIndex == 0 || columnIndex == 1 : columnIndex;
    rectangle.y = 0;
    rectangle.height = getHeight(context.getWidth(), context);
    RendererActivity activity = getCell(columnIndex).getActivityAt(id, x, y, context, rectangle);
    if (activity != null)
      activity.storeValue(COLUMN, columnIndex);
    return activity;
  }

  public boolean getLineRectangle(int y, Rectangle lineRectangle, RendererContext context) {
    int width2 = mySecond.getWidth(context);
    lineRectangle.width = myTable.getColumnX(1, context) + width2;
    return true;
  }

  @Nullable
  public JComponent getNextLiveComponent(@NotNull RendererContext context, TableRenderer renderer,
    @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next)
  {
    Integer firstColumn;
    if (current != null)
      firstColumn = COLUMN.getClientValue(current);
    else
      firstColumn = -1;
    if (firstColumn == null) {
      assert false : current;
      firstColumn = -1;
    }
    if (firstColumn == -1) {
      firstColumn = next ? 0 : getCellCount() - 1;
      current = null;
    } else {
      if (firstColumn != 0 && firstColumn != 1) {
        assert false : firstColumn;
        firstColumn = 1;
      }
    }
    int savedY = targetArea.y;
    int savedHeight = targetArea.height;
    for (int i = firstColumn; next ? i < getCellCount() : i >= 0; i += next ? 1 : -1) {
      targetArea.x = renderer.getColumnX(i, context);
      targetArea.width = renderer.getColumnWidth(i, context);
      targetArea.y = savedY;
      targetArea.height = savedHeight;
      JComponent result = getCell(i).getNextLifeComponent(context, current, targetArea, next);
      if (result != null) {
        COLUMN.putClientValue(result, i);
        return result;
      }
      current = null;
    }
    return null;
  }

  private TableRendererCell getCell(int column) {
    assert column == 0 || column == 1 : column;
    return column == 0 ? myFirst : mySecond;
  }

  private int getCellCount() {
    return 2;
  }

  public int getPreferedWidth(int column, RendererContext context) {
    return -1;
  }

  public static TwoColumnLine labeledCell(String label, TableRendererCell cell, TableRenderer renderer) {
    return new TwoColumnLine(TextCell.label(label), cell, renderer);
  }
}
