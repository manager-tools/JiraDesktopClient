package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.impl.HostComponentState;
import com.almworks.util.ui.widgets.impl.WidgetHostComponent;
import com.almworks.util.ui.widgets.util.ActiveCellCollector;
import com.almworks.util.ui.widgets.util.ScrollPolicy;
import com.almworks.util.ui.widgets.util.ScrollWidget;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BigGrid implements Widget<Object> {
  private final Dimension myPrefSize = new Dimension();
  private final Dimension myGridSize = new Dimension();
  private final Widget<?> myChild;
  private final ActiveCellCollector myCells = new ActiveCellCollector();

  public BigGrid(Widget<?> child, int width, int height, int columns, int rows) {
    myChild = child;
    myPrefSize.setSize(width, height);
    myGridSize.setSize(columns, rows);
  }

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable Object value) {
    return myPrefSize.width;
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable Object value) {
    return myPrefSize.height;
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable Object value) {}

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable Object value, TypedKey<?> reason) {
    if (reason == EventContext.CELL_INVALIDATED) {
      context.consume();
    } else if (reason == FocusTraverse.KEY) {
      //noinspection ConstantConditions
      context.getData(FocusTraverse.KEY).defaultTraverse(context, 0, myGridSize.width * myGridSize.height - 1);
    }
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable Object value) {
    return value;
  }

  @Override
  public void layout(LayoutContext context, Object value, @Nullable ModifiableHostCell cell) {
    if (cell != null) {
      layoutCell(context, cell);
      return;
    }
    int cellHeight = getCellHeight(context);
    int y = context.getTargetY();
    y -= y % cellHeight;
    int r = y / cellHeight;
    while (y < context.getTargetY() + context.getTargetHeight()) {
      layoutRow(context, r, y, cellHeight);
      r++;
      y += cellHeight;
    }
  }

  private void layoutRow(LayoutContext context, int row, int y, int cellHeight) {
    int cellWidth = getCellWidth(context);
    int x = context.getTargetX();
    x -= x % cellWidth;
    int c = x / cellWidth;
    int id = row * myGridSize.width + c;
    int targetCornerX = context.getTargetX() + context.getTargetWidth();
    while (x < targetCornerX) {
      context.setChildBounds(id, myChild, x, y, cellWidth, cellHeight);
      id++;
      c++;
      x += cellWidth;
    }
  }

  private void layoutCell(LayoutContext context, ModifiableHostCell cell) {
    int cellWidth = getCellWidth(context);
    int cellHeight = getCellHeight(context);
    int id = cell.getId();
    int r = id / myGridSize.width;
    int c = id - r;
    context.setChildBounds(id, myChild, c*cellWidth, r*cellHeight, cellWidth, cellHeight);
  }

  private int getCellHeight(LayoutContext context) {
    return context.getHeight() / myGridSize.height;
  }

  private int getCellWidth(LayoutContext context) {
    return context.getWidth() / myGridSize.width;
  }

  @Override
  public WidgetAttach getAttach() {
    return myChild.getAttach();
  }

  @Override
  public CellActivate getActivate() {
    return myCells;
  }

  @Override
  public void updateUI(HostCell cell) {}

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable(){
      @Override
      public void run() {
        WidgetHostComponent host = new WidgetHostComponent();
        final HostComponentState<Object> state = host.createState();
        final ScrollWidget<Object> scroll = ScrollWidget.wrap(new BigGrid(new FocusBorderWidget(), 10000, 10000, 500, 1000), new ScrollPolicy.Fixed(SizeCalculator1D.fixedPixels(1), true), true);
        state.setWidget(scroll);
        state.setValue(null);
        final JFrame frame = DebugFrame.show(host);
        frame.setSize(1000, 800);
        host.setState(state);
//        SwingUtilities.invokeLater(new Runnable() {
//          private int delta = 1;
//          private int count = 0;
//          private long start = System.currentTimeMillis();
//          @Override
//          public void run() {
//            int width = frame.getWidth();
//            if (width >= 1200) delta = -1;
//            else if (width <= 500) delta = 1;
//            int height = frame.getHeight();
//            frame.setSize(width + delta, height + delta);
//            count++;
//            if (count % 50 == 0) {
//              long now = System.currentTimeMillis();
//              System.out.println(now - start);
//              start = now;
//            }
//            SwingUtilities.invokeLater(this);
//          }
//        });

//        SwingUtilities.invokeLater(new Runnable(){
//          private int delta = 1;
//          private int count = 0;
//          private long start = 0;
//          @Override
//          public void run() {
//            BoundedRangeModel vert = scroll.getScrollBar(state.getRootCell(), true).getModel();
//            if (vert != null) {
//              if (start == 0) start = System.currentTimeMillis();
//              int extent = vert.getExtent();
//              int value = vert.getValue();
//              int max = vert.getMaximum();
//              int min = vert.getMinimum();
//              int newValue = value + delta;
//              if (newValue < min || newValue + extent > max) {
//                delta = -delta;
//                run();
//                return;
//              }
//              vert.setValue(newValue);
//              count++;
//              if (count % 30 == 0) {
//                long now = System.currentTimeMillis();
//                System.out.println(now - start);
//                start = now;
//              }
//            }
//            SwingUtilities.invokeLater(this);
//          }
//        });
      }
    });
  }
}
