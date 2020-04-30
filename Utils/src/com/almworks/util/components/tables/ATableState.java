package com.almworks.util.components.tables;

import com.almworks.util.models.TableColumnAccessor;
import org.jetbrains.annotations.NotNull;

public interface ATableState<T> {
  /**
   * @return 1 - direct sort<p> 0 - not sorted by this column <p> -1 - reverse
   * @param columnAccessor
   */
  int getColumnSortState(@NotNull TableColumnAccessor<T,?> columnAccessor);

  TableColumnAccessor<? super T, ?> getColumnAccessor(int modelIndex);

  void sortBy(TableColumnAccessor<? super T, ?> column, boolean reverse);

//    SubsetModel<TableColumnAccessor<T, ?>> getColumnsSubsetModel();
}
