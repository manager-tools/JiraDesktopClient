package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.ArrayUtil;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GridWidget<T> implements Widget<T> {
  private static final Log<GridWidget<?>> log = (Log)Log.get(GridWidget.class);
  private final SegmentsLayout myColumns = new SegmentsLayout(1, 1);
  private final SegmentsLayout myRows = new SegmentsLayout(1, 1);
  private final WidgetChildList<T> myChildren = new WidgetChildList<T>();
  private final ActiveCellCollector myCells = new ActiveCellCollector();

  public void setLayout(@Nullable SegmentsLayout columns, @Nullable SegmentsLayout rows) {
    if (columns != null) myColumns.setSegments(columns);
    if (rows != null) myRows.setSegments(columns);
    revalidate();
  }

  public void setRowWeights(int row, int grow, int shrink) {
    myRows.setSegmentPolicy(row, grow, shrink);
    revalidate();
  }

  public void setColumnWeights(int column, int grow, int shrink) {
    myColumns.setSegmentPolicy(column, grow, shrink);
    revalidate();
  }

  public void addChild(Widget<? super T> child) {
    myChildren.addChild(child);
    revalidate();
  }

  public void setChild(int[] columnRow, Widget<? super T> child) {
    setChild(columnRow[0], columnRow[1], child);
  }

  public void setChild(int column, int row, Widget<? super T> child) {
    if (column >= myColumns.getSegmentCount() || row >= myRows.getSegmentCount()) throw new IllegalArgumentException(column + " " + row);
    int index = column + row * myColumns.getSegmentCount();
    while (index > myChildren.size()) myChildren.addChild(EMPTY_WIDGET);
    myChildren.setChild(index, child);
    myCells.deleteChild(index);
    revalidate();
  }

  @Nullable
  public Widget<? super T> getChildWidget(int column, int row) {
    if (column >= myColumns.getSegmentCount() || row >= myRows.getSegmentCount()) throw new IllegalArgumentException(column + " " + row);
    int index = column + row * myColumns.getSegmentCount();
    return index < myChildren.size() ? myChildren.get(index) : null;
  }

  public void setDimension(int columns, int rows) {
    if (columns > 0) myColumns.setSegmentCount(columns);
    if (rows > 0) myRows.setSegmentCount(rows);
    revalidate();
  }

  @Nullable
  public HostCell findCell(@Nullable HostCell gridCell, int[] columnRow) {
    return gridCell == null ? null : findCell(gridCell, columnRow[0], columnRow[1]);
  }

  @Nullable
  public HostCell findCell(HostCell gridCell, int column, int row) {
    if (gridCell == null) return null;
    if (gridCell.getWidget() != this) {
      log.error(this, "findCell", gridCell.getWidget(), gridCell, column, row);
      return null;
    }
    if (column < 0 || row < 0) {
      log.error(this, "findCell", column, row);
      return null;
    }
    int columnCount = myColumns.getSegmentCount();
    if (column >= columnCount || row >= myRows.getSegmentCount()) {
      log.error(this, "findCell", column, row, myColumns.getSegmentCount(), myRows.getSegmentCount());
      return null;
    }
    return gridCell.findChild(row * columnCount + column);
  }

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable T value) {
    int[] widths = calcPrefWidth(context, value);
    return ArrayUtil.sum(widths, 0, myColumns.getSegmentCount());
  }

  private int[] calcPrefWidth(CellContext context, T value) {
    int rows = myRows.getSegmentCount();
    int columnCount = myColumns.getSegmentCount();
    int[] widths = new int[columnCount];
    for (int c = 0; c < columnCount; c++) {
      int max = 0;
      for (int i = c; i < myChildren.size(); i += rows) {
        Widget<? super T> child = myChildren.get(i);
        max = Math.max(max, context.getChildPreferedWidth(i, child, value));
      }
      widths[c] = max;
    }
    return widths;
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable T value) {
    int[] widths = calcPrefWidth(context, value);
    myColumns.layout(width, widths, myColumns.getSegmentCount(), 0);
    int[] heights = calcPrefHeights(context, value, widths);
    return ArrayUtil.sum(heights, 0, myRows.getSegmentCount());
  }

  private int[] calcPrefHeights(CellContext context, T value, int[] widths) {
    int columnCount = myColumns.getSegmentCount();
    int rowCount = myRows.getSegmentCount();
    int[] heights = new int[rowCount];
    for (int r = 0; r < rowCount; r++) {
      int firstCell = r * columnCount;
      int max = 0;
      for (int i = firstCell; i < Math.min((r + 1)* columnCount, myChildren.size()); i++) {
        Widget<? super T> child = myChildren.get(i);
        max = Math.max(max, context.getChildPreferedHeight(i, child, value, widths[i - firstCell]));
      }
      heights[r] = max;
    }
    return heights;
  }


  @Override
  public void paint(@NotNull GraphContext context, @Nullable T value) {}

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (reason == FocusTraverse.KEY) //noinspection ConstantConditions
      context.getData(FocusTraverse.KEY).defaultTraverse(context, 0, myChildren.size() - 1);
    else if (reason == EventContext.CELL_INVALIDATED) revalidate(context);
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    return value;
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {
    int[] widths = calcPrefWidth(context, value);
    int columnCount = myColumns.getSegmentCount();
    myColumns.layout(context.getWidth(), widths, columnCount, 0);
    int[] heights = calcPrefHeights(context, value, widths);
    myRows.layout(context.getHeight(), heights, myRows.getSegmentCount(), 0);
    for (int i = 0; i < myChildren.size(); i++) {
      int r = i / columnCount;
      int c = i % columnCount;
      Widget<? super T> child = myChildren.get(i);
      int x = ArrayUtil.sum(widths, 0, c);
      int y = ArrayUtil.sum(heights, 0, r);
      context.setChildBounds(i, child, x, y, widths[c], heights[r], null);
    }
  }

  @Override
  public WidgetAttach getAttach() {
    return myChildren;
  }

  @Override
  public CellActivate getActivate() {
    return myCells;
  }

  @Override
  public void updateUI(HostCell cell) {}

  private void revalidate() {
    myCells.revalidate();
  }

  private void revalidate(CellContext context) {
    context.invalidate();
  }
}
