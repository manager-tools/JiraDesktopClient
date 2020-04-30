package com.almworks.util.models;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.ATableModel;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.components.TableCellState;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Arrays;

public interface ColumnSizePolicy {
  public static final ColumnSizePolicy FIXED = new DefaultColumnSizePolicy(false, false);
  public static final ColumnSizePolicy FREE = new DefaultColumnSizePolicy(true, true);
  public static final ColumnSizePolicy SRINKABLE = new DefaultColumnSizePolicy(true, false);
  public static final ColumnSizePolicy GROW_ONLY = new DefaultColumnSizePolicy(false, true);

  void setWidthParameters(int columnWidth, TableColumn column);

  <E> int getPreferredWidth(JTable table, ATableModel<E> model, ColumnAccessor<E> column, int columnIndex);

  /**
   * When user forces column to this width, must return either nearest valid width or -1
   */
  int validateForcedWidth(TableColumn column, int width);

  class Sum implements ColumnSizePolicy {
    private final SizeCalculator1D[] mySummands;
    private final ColumnSizePolicy myBehaviour;

    public Sum(SizeCalculator1D[] summands, ColumnSizePolicy behaviour) {
      mySummands = summands;
      myBehaviour = behaviour;
    }

    public void setWidthParameters(int columnWidth, TableColumn column) {
      myBehaviour.setWidthParameters(columnWidth, column);
    }

    public <E> int getPreferredWidth(JTable table, ATableModel<E> model, ColumnAccessor<E> column, int columnIndex) {
      int sum = 0;
      for (SizeCalculator1D summand : mySummands)
        sum += summand.getPrefLength(table);
      return sum;
    }

    public int validateForcedWidth(TableColumn column, int width) {
      return myBehaviour.validateForcedWidth(column, width);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      Sum other = Util.castNullable(Sum.class, obj);
      return other != null && Arrays.equals(mySummands, other.mySummands) && Util.equals(myBehaviour, other.myBehaviour);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myBehaviour) ^ mySummands.length ^ Sum.class.hashCode();
    }
  }


  class Calculated implements ColumnSizePolicy {
    private final ColumnSizePolicy myBehaviour;
    private final SizeCalculator1D myCalculator;

    public Calculated(SizeCalculator1D calculator, ColumnSizePolicy behaviour) {
      myCalculator = calculator;
      myBehaviour = behaviour;
    }

    public void setWidthParameters(int columnWidth, TableColumn column) {
      myBehaviour.setWidthParameters(columnWidth, column);
    }

    public <E> int getPreferredWidth(JTable table, ATableModel<E> model, ColumnAccessor<E> column, int columnIndex) {
      return myCalculator.getPrefLength(table);
    }

    public int validateForcedWidth(TableColumn column, int width) {
      return myBehaviour.validateForcedWidth(column, width);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      Calculated other = Util.castNullable(Calculated.class, obj);
      return other != null && Util.equals(myBehaviour, other.myBehaviour) && Util.equals(myCalculator, other.myCalculator);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myBehaviour, myCalculator) ^ Calculated.class.hashCode();
    }

    public static ColumnSizePolicy free(SizeCalculator1D calculator) {
      return new Calculated(calculator, ColumnSizePolicy.FREE);
    }

    public static ColumnSizePolicy freeLetterMWidth(int count) {
      return free(SizeCalculator1D.letterMWidth(count));
    }

    public static ColumnSizePolicy fixedPixels(int pixels) {
      SizeCalculator1D calculator = SizeCalculator1D.fixedPixels(pixels);
      return fixed(calculator);
    }

    public static Calculated fixed(SizeCalculator1D calculator) {
      return new Calculated(calculator, ColumnSizePolicy.FIXED);
    }

    public static ColumnSizePolicy fixedTextWithMargin(String text, int letterMmargin) {
      return new Sum(new SizeCalculator1D[] {SizeCalculator1D.text(text), SizeCalculator1D.letterMWidth(letterMmargin)},
        ColumnSizePolicy.FIXED);
    }
  }


  class DefaultColumnSizePolicy implements ColumnSizePolicy {
    static final int MIN_WIDTH = 15;
    static final int MAX_WIDTH = Short.MAX_VALUE;
    private static final int MAXIMUM_AUTOSIZE_ROWS = 200;

    private final boolean myCanShrink;
    private final boolean myCanGrow;

    private DefaultColumnSizePolicy(boolean canShrink, boolean canGrow) {
      myCanShrink = canShrink;
      myCanGrow = canGrow;
    }

    public <E> int getPreferredWidth(JTable table, ATableModel<E> model, ColumnAccessor<E> column, int columnIndex) {
      // This lines are commented out because of tables that are shown in own windows. Window prefered size is calculated before component becomes displayable.
//      if (!table.isDisplayable())
//        return -1;
      AListModel<E> data = model.getDataModel();
      int[] range = {0, data.getSize()};
      if (range[1] <= 0) {
        return -1;
      }
      if (range[1] > MAXIMUM_AUTOSIZE_ROWS) {
        if (!getNearestAutosizeRange(table, data, range)) {
          return -1;
        }
      }
      assert range[0] < range[1] : range[0] + " " + range[1];
      assert range[1] - range[0] <= MAXIMUM_AUTOSIZE_ROWS : range[0] + " " + range[1];

      CollectionRenderer<? super E> renderer = column.getDataRenderer();
      int minimum = 15;
      for (int i = range[0]; i < range[1]; i++) {
        TableCellState state = new TableCellState(table, true, true, i, columnIndex);
        // todo do we need to make c displayable?
        JComponent c = renderer.getRendererComponent(state, data.getAt(i));
        Dimension preferredSize = c.getPreferredSize();
        if (preferredSize != null) {
          minimum = Math.max(minimum, preferredSize.width + 8);
        }
      }

      return minimum;
    }

    public int validateForcedWidth(TableColumn column, int width) {
      if (width <= 0)
        return -1;
      int pref = column.getPreferredWidth();
      if (width == pref)
        return width;
      if (width < pref)
        return myCanShrink ? Math.max(width, MIN_WIDTH) : pref;
      else
        return myCanGrow ? Math.min(width, MAX_WIDTH) : pref;
    }

    private boolean getNearestAutosizeRange(JTable table, AListModel<?> data, int[] range) {
      JViewport viewport = null;
      Container parent = table.getParent();
      if (parent == null)
        return false;
      if (parent instanceof JViewport) {
        viewport = (JViewport) parent;
      } else {
        parent = parent.getParent();
        if (parent instanceof JViewport) {
          viewport = (JViewport) parent;
        }
      }
      if (viewport == null)
        return false;
      Rectangle rect = viewport.getViewRect();
      Component view = viewport.getView();
      if (view != table) {
        rect = SwingUtilities.convertRectangle(view, rect, table);
      }
      int start = Math.max(0, table.rowAtPoint(rect.getLocation()));
      int end = table.rowAtPoint(new Point(rect.x, rect.y + rect.width - 2)) + 1;
      int add = MAXIMUM_AUTOSIZE_ROWS - (end - start);
      if (add < 0) {
        end -= (-add);
      } else {
        int d = add / 3;
        start -= d;
        end += (add - d);
      }
      if (start < 0) {
        end += (-start);
        start = 0;
      }
      if (end > data.getSize()) {
        end = data.getSize();
      }
      range[0] = start;
      range[1] = end;
      return true;
    }

    private int getMinimumWidth(int preferredWidth) {
      return myCanShrink ? MIN_WIDTH : preferredWidth;
    }

    private int getMaximumWidth(int preferredWidth) {
      return myCanGrow ? MAX_WIDTH : preferredWidth;
    }

    public void setWidthParameters(int preferredWidth, TableColumn column) {
      int newMin = getMinimumWidth(preferredWidth);
      int newMax = getMaximumWidth(preferredWidth);
      if (newMin > column.getMaxWidth()) {
        column.setMaxWidth(newMax);
        column.setMinWidth(newMin);
      } else {
        column.setMinWidth(newMin);
        column.setMaxWidth(newMax);
      }
      column.setPreferredWidth(preferredWidth);
      boolean resizable = myCanShrink || myCanGrow;
      if (resizable != column.getResizable()) column.setResizable(resizable);
    }
  }
}
