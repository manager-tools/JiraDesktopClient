package com.almworks.sumtable;

import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class SummaryTableData {
  public static final DataRole<SummaryTableData> DATA = DataRole.createRole(SummaryTableData.class);
  
  protected final List<STFilter> myColumns;
  protected final List<STFilter> myRows;
  protected final List<STFilter> myCounters;

  protected SummaryTableData(List<STFilter> columns, List<STFilter> rows, List<STFilter> counters) {
    myColumns = columns;
    myRows = rows;
    myCounters = counters;
  }

  public List<STFilter> getRows() {
    return myRows;
  }

  public List<STFilter> getColumns() {
    return myColumns;
  }

  public List<STFilter> getCounters() {
    return myCounters;
  }

  @Nullable
  @ThreadAWT
  public abstract Integer getCellCount(int counterIndex, int rowIndex, int columnIndex);

  public abstract boolean isDataAvailable();
}
