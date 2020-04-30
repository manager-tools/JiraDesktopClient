package com.almworks.util.ui.widgets.util.list;

import com.almworks.integers.IntArray;
import com.almworks.util.ui.widgets.HostCell;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Log;

public class ListLayoutState {
  private final int[] myWidth;
  private final IntArray myHeight;
  private final ColumnListWidget<?> myWidget;

  public ListLayoutState(int columnCount, ColumnListWidget<?> widget) {
    myWidget = widget;
    myWidth = new int[columnCount];
    myHeight = new IntArray();
  }

  void onLayout(int[] width, int[] heights, HostCell cell) {
    if (myWidth.length != width.length) {
      Log.error("Wrong number of columns for layout");
      return;
    }
    if (!hasChanges(width, heights)) return;
    System.arraycopy(width, 0, myWidth, 0, myWidth.length);
    myHeight.clear();
    myHeight.addAll(heights);
    myWidget.notifyLayoutChanged(cell);
  }

  private boolean hasChanges(int[] columnWidths, int[] heights) {
    for (int i = 0; i < myWidth.length; i++) if (myWidth[i] != columnWidths[i]) return true;
    if (myHeight.size() != heights.length) return true;
    for (int i = 0; i < myHeight.size(); i++) if (myHeight.get(i) != heights[i]) return true;
    return false;
  }

  public int[] copyCurrentWidth() {
    return ArrayUtil.arrayCopy(myWidth);
  }

  public int[] copyCurrentHeights() {
    return myHeight.toNativeArray();
  }

  public int getRowCount() {
    return myHeight.size();
  }

  public int[] getRowBounds(int row) {
    if (row < 0 || row >= myHeight.size()) return null;
    int offset = 0;
    for (int i = 0; i < row; i++) offset += myHeight.get(i);
    return new int[] {offset, myHeight.get(row)};
  }
}
