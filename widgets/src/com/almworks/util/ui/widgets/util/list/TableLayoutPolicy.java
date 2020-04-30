package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.ui.widgets.CellContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TableLayoutPolicy {
  @NotNull
  int[] getMargin(int[] rowHeights, int rowIndex, @Nullable int[] target);

  <T> int getPreferedCellHeight(CellContext context, ColumnListWidget<T> listWidget, int rowIndex, T rowValue, int[] width,
    int totalRowCount);

  void invalidateLayoutCache(CellContext context);
}
