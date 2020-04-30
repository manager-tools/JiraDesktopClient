package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.GraphContext;
import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.util.LeafRectCell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ColumnBreakWidget extends LeafRectCell<Object> {
  private final int myWidth;
  private final int myBreakX;

  public ColumnBreakWidget(int width, int breakX) {
    myWidth = width;
    myBreakX = breakX;
  }

  @NotNull
  @Override
  protected Dimension getPrefSize(CellContext context, Object value) {
    return new Dimension(myWidth, 1);
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable Object value) {
    int x = myBreakX;
    int height = context.getHeight();

//    Graphics2D g = context.getGraphics();
//    JComponent component = context.getHost().getHostComponent();
//    context.setColor(ColorUtil.between(component.getBackground(), component.getForeground(), 0.2f));
//    g.drawLine(x, 0, x, height);
//    BrokenLineBorder.drawVLine(context.getGraphics(), x, 0, height, BrokenLineBorder.DOTTED);
  }

  public int getLeftWidth() {
    return Math.max(0, Math.min(myWidth, myBreakX));
  }

  public int getRightWidth() {
    return Math.max(0, Math.min(myWidth, myWidth - myBreakX));
  }

  public static class ColumnIterator {
    private final Widget<?>[] myWidgets;
    private final int[] myWidths;
    private int myLast = -1;

    public ColumnIterator(Widget<?>[] widgets, int[] widths) {
      myWidgets = widgets;
      myWidths = widths;
    }

    public int nextColumnWidth() {
      if (myLast >= myWidgets.length) return 0;
      int next = findNext(myWidgets, myLast + 1);
      int sum;
      if (myLast >= 0) sum = ((ColumnBreakWidget) myWidgets[myLast]).getRightWidth();
      else sum = 0;
      for (int i = myLast + 1; i < next; i++) sum += myWidths[i];
      if (next < myWidgets.length) sum += ((ColumnBreakWidget) myWidgets[next]).getLeftWidth();
      myLast = next;
      return sum;
    }
  }

  public static int findNext(Widget<?>[] widgets, int fromIndex) {
    for (int i = fromIndex; i < widgets.length; i++) {
      if (widgets[i] instanceof ColumnBreakWidget) return i;
    }
    return widgets.length;
  }
}
