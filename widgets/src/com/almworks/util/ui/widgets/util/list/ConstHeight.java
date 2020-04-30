package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.GraphContext;
import com.almworks.util.ui.widgets.Widget;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Table {@link TableLayoutPolicy layout} and {@link TablePaintPolicy paint} policy.<br>
 * Layouts table with equal height rows. Each row has equal top and bottom margin.<br>
 * Paints each row via (optional) {@link RowPaintPolicy}.
 */
public class ConstHeight implements TableLayoutPolicy, TablePaintPolicy {
  private static final TypedKey<Integer> RAW_CELL_HEIGHT = TypedKey.create("rawCellHeight");
  @Nullable
  private final RowPaintPolicy myRowPaint;
  private final int myTopInset;
  private final int myBottomInset;

  /**
   * @param topInset top row margin
   * @param bottomInset bottom row margin
   * @param rowPaint optional row painter. If null nothing is painted.
   */
  public ConstHeight(int topInset, int bottomInset, @Nullable RowPaintPolicy rowPaint) {
    myTopInset = topInset;
    myBottomInset = bottomInset;
    myRowPaint = rowPaint;
  }

  @Override
  public void paintTable(GraphContext context, ColumnListWidget.CellState state) {
    if (myRowPaint == null) return;
    Integer h = context.getStateValue(RAW_CELL_HEIGHT);
    if (h == null) return;
    int height = h + myBottomInset + myTopInset;
    Rectangle clip = context.getLocalClip(null);
    int y = 0;
    int i = 0;
    while (true) {
      if (y + height >= clip.y) myRowPaint.paint(context, i, y, height);
      y += height;
      if (y > clip.y + clip.height) break;
      i++;
    }
  }
  
  @Override
  public int[] getMargin(int[] rowHeights, int rowIndex, int[] target) {
    return getMargin(rowHeights.length, rowIndex, target);
  }

  private int[] getMargin(int rowCount, int rowIndex, int[] target) {
    if (target == null) target = new int[2];
    target[0] = myTopInset;
    target[1] = myBottomInset;
    return target;
  }

  @Override
  public <T> int getPreferedCellHeight(CellContext context, ColumnListWidget<T> listWidget, int rowIndex, T rowValue,
    int[] widths, int totalRowCount) {
    int height = getCachedPrefHeight(context, listWidget, rowIndex, rowValue, widths);
    int[] margin = getMargin(totalRowCount, rowIndex, null);
    return height + margin[0] + margin[1];
  }

  private <T> int getCachedPrefHeight(CellContext context, ColumnListWidget<T> listWidget, int rowIndex, T rowValue, int[] widths) {
    Integer height = context.getStateValue(RAW_CELL_HEIGHT);
    if (height == null) {
      height = calcPrefHeight(context, listWidget, rowIndex, rowValue, widths);
      context.putStateValue(RAW_CELL_HEIGHT, height, true);
    }
    if (height == null) return 0;
    return height;
  }

  private <T> int calcPrefHeight(CellContext context, ColumnListWidget<T> listWidget, int rowIndex, T rowValue, int[] widths) {
    int result = 0;
    for (int c = 0; c < listWidget.getColumnCount(); c++) {
      Widget<T> columnWidget = listWidget.getColumnWidget(c);
      int h = context.getChildPreferedHeight(listWidget.getCellId(c, rowIndex), columnWidget, rowValue, widths[c]);
      result = Math.max(result, h);
    }
    return result;
  }

  @Override
  public void invalidateLayoutCache(CellContext context) {
    context.putStateValue(RAW_CELL_HEIGHT, null, true);
  }

  private int getVerticalInset() {
    return myTopInset + myBottomInset;
  }
}
