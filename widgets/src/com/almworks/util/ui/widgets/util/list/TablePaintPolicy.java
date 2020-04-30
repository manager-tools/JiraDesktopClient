package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.ui.widgets.GraphContext;

public interface TablePaintPolicy {
  void paintTable(GraphContext context, ColumnListWidget.CellState state);
}
