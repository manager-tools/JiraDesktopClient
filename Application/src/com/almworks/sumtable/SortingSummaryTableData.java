package com.almworks.sumtable;


import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

class SortingSummaryTableData extends SummaryTableData {
  private final SummaryTableData mySource;
  private final int[] myRowPermutation;
  private final int[] myColumnPermutation;

  public SortingSummaryTableData(SummaryTableData source, int[] rowPermutation, int[] columnPermutation) {
    super(permuteList(source.getColumns(), columnPermutation), permuteList(source.getRows(), rowPermutation), source.getCounters());
    mySource = source;
    myRowPermutation = rowPermutation;
    myColumnPermutation = columnPermutation;
  }

  public boolean isDataAvailable() {
    return mySource.isDataAvailable();
  }

  private static List<STFilter> permuteList(List<STFilter> list, int[] permutation) {
    if (permutation == null)
      return list;
    int length = permutation.length;
    if (list.size() != length) {
      assert false : list + " " + Arrays.toString(permutation);
      return list;
    }
    List<STFilter> result = Collections15.arrayList(list.size());
    for (int i = 0; i < length; i++) {
      int idx = permutation[i];
      if (idx < 0 || idx >= length) {
        assert false : idx + " " + i + " " + permutation;
        return list;
      }
      result.add(i, list.get(idx));
    }
    return result;
  }

  @Nullable
  @ThreadAWT
  public Integer getCellCount(int counterIndex, int rowIndex, int columnIndex) {
    if (myRowPermutation != null) {
      if (rowIndex < 0 || rowIndex >= myRowPermutation.length)
        return null;
      rowIndex = myRowPermutation[rowIndex];
    }
    if (myColumnPermutation != null) {
      if (columnIndex < 0 || columnIndex >= myColumnPermutation.length)
        return null;
      columnIndex = myColumnPermutation[columnIndex];
    }
    return mySource.getCellCount(counterIndex, rowIndex, columnIndex);
  }
}
