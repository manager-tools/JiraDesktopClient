package com.almworks.util.components.layout;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Array2D;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.IntArray;
import com.almworks.util.components.renderer.ComponentCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Failure;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Arrays;

/**
 * {@link InlineLayout.HORISONTAL} means columns<br>
 * {@link InlineLayout.VERTICAL} mean rows
 *
 * @author dyoma
 */
@SuppressWarnings({"JavadocReference"})
public class LightTableLayout<T> extends JComponent {
  private static final int MAX_VALUE = Short.MAX_VALUE;
  @NotNull
  private AListModel<?>[] myModels = new AListModel[] {AListModel.EMPTY, AListModel.EMPTY};
  private final CellSizes[] mySizeCache = new LightTableLayout.CellSizes[4];
  private final MyListListener[] myListeners = new LightTableLayout.MyListListener[] {
    new MyListListener(InlineLayout.HORISONTAL), new MyListListener(InlineLayout.VERTICAL)};

  public LightTableLayout() {
    Arrays.fill(myModels, AListModel.EMPTY);
    mySizeCache[0] = new CellSizeLayout();
    mySizeCache[1] = new CellSizeCache(InlineLayout.PREF_SIZE);
    mySizeCache[2] = new CellSizeCache(InlineLayout.MAX_SIZE);
    mySizeCache[3] = new CellSizeCache(InlineLayout.MIN_SIZE);
  }

  private JComponent getRendererComponent(InlineLayout.Orientation orientation, int pri, int sec) {
    int columnIndex;
    int rowIndex;
    if (orientation == InlineLayout.HORISONTAL) {
      columnIndex = pri;
      rowIndex = sec;
    } else if (orientation == InlineLayout.VERTICAL) {
      columnIndex = sec;
      rowIndex = pri;
    } else
      throw new Failure(String.valueOf(orientation));
    ColumnAccessor<T> column = (ColumnAccessor<T>) myModels[0].getAt(columnIndex);
    T row = (T) myModels[1].getAt(rowIndex);
    return column.getDataRenderer().getRendererComponent(new MyCellState(columnIndex, rowIndex), row);
  }

  public Dimension getPreferredSize() {
    return calcSize(InlineLayout.PREF_SIZE);
  }

  public Dimension getMaximumSize() {
    return calcSize(InlineLayout.MAX_SIZE);
  }

  public Dimension getMinimumSize() {
    return calcSize(InlineLayout.MIN_SIZE);
  }

  private Dimension calcSize(Convertor<Component, Dimension> aspect) {
    LightTableLayout<T>.CellSizes cellSizes = getCachedSizes(aspect);
    Dimension result =
      new Dimension(cellSizes.getTotal(InlineLayout.HORISONTAL), cellSizes.getTotal(InlineLayout.VERTICAL));
    AwtUtil.addInsets(result, getInsets());
    return result;
  }

  private CellSizes getCachedSizes(Convertor<Component, Dimension> aspect) {
    if (aspect == null)
      return mySizeCache[0];
    else if (aspect == InlineLayout.PREF_SIZE)
      return mySizeCache[1];
    else if (aspect == InlineLayout.MAX_SIZE)
      return mySizeCache[2];
    else if (aspect == InlineLayout.MIN_SIZE)
      return mySizeCache[3];
    throw new Failure(String.valueOf(aspect));
  }

  private abstract class CellSizes {
    private final IntArray[] myMaxSizes = new IntArray[] {new IntArray(), new IntArray()};

    public int getTotal(InlineLayout.Orientation orientation) {
      int total = 0;
      IntArray priSizes = myMaxSizes[orientation.getIndex()];
      for (int i = 0; i < priSizes.size(); i++) {
        int pri = getMaxLineSize(orientation, i);
        if (pri == MAX_VALUE)
          return MAX_VALUE;
        total += pri;
      }
      return total;
    }

    public void invalidate(InlineLayout.Orientation orientation, int index0, int index1) {
      IntArray array = myMaxSizes[orientation.getIndex()];
      int low = Math.min(index0, index1);
      int high = Math.max(index0, index1);
      for (int i = low; i <= high; i++)
        array.set(i, -1);
    }

    public void insertLines(InlineLayout.Orientation orientation, int index0, int index1) {
      IntArray array = myMaxSizes[orientation.getIndex()];
      int low = Math.min(index0, index1);
      int high = Math.max(index0, index1);
      array.insertRange(low, high + 1, -1);
    }

    public void removeLines(InlineLayout.Orientation orientation, int index0, int index1) {
      IntArray array = myMaxSizes[orientation.getIndex()];
      int low = Math.min(index0, index1);
      int high = Math.max(index0, index1);
      array.removeRange(low, high + 1);
    }

    public abstract Dimension getCellSize(InlineLayout.Orientation orientation, int pri, int sec);

    private void setMaxValue(IntArray array, int index, int value) {
      array.set(index, Math.min(MAX_VALUE, Math.max(0, Math.max(array.get(index), value))));
    }

    public int getLineStart(InlineLayout.Orientation orientation, int index) {
      int total = 0;
      for (int i = 0; i < index; i++) {
        total += getMaxLineSize(orientation, index);
        total = Math.min(MAX_VALUE, total);
      }
      return total;
    }

    protected int getLineWidth(InlineLayout.Orientation orientation, int index) {
      return myMaxSizes[orientation.getIndex()].get(index);
    }

    private int getMaxLineSize(InlineLayout.Orientation orientation, int lineIndex) {
      IntArray priSizes = myMaxSizes[orientation.getIndex()];
      int size = priSizes.get(lineIndex);
      if (size == -1) {
        IntArray secSizes = myMaxSizes[orientation.getOpposite().getIndex()];
        for (int i = 0; i < secSizes.size(); i++) {
          Dimension cellSize = getCellSize(orientation, lineIndex, i);
          setMaxValue(priSizes, lineIndex, orientation.getAlong(cellSize));
          setMaxValue(secSizes, i, orientation.getAcross(cellSize));
        }
        size = priSizes.get(lineIndex);
      }
      assert size >= 0 : size;
      return size;
    }
  }


  private class CellSizeCache extends CellSizes {
    private final Convertor<Component, Dimension> mySizeAspect;
    private final Array2D<Dimension> mySizes = new Array2D<Dimension>();

    public CellSizeCache(Convertor<Component, Dimension> sizeAspect) {
      mySizeAspect = sizeAspect;
    }

    public Dimension getCellSize(InlineLayout.Orientation orientation, int pri, int sec) {
      Dimension cellSize = mySizes.get(pri, sec);
      if (cellSize == null) {
        Dimension cellSize1;
        JComponent component1 = getRendererComponent(orientation, pri, sec);
        cellSize1 = mySizeAspect.convert(component1);
        cellSize = cellSize1;
        mySizes.set(pri, sec, cellSize);
      }
      return cellSize;
    }

    public void invalidate(InlineLayout.Orientation orientation, int index0, int index1) {
      super.invalidate(orientation, index0, index1);
      assert index0 <= index1 : index0 + " " + index1;
      if (orientation == InlineLayout.HORISONTAL)
        mySizes.fillColumns(index0, index1 + 1, null);
      else if (orientation == InlineLayout.VERTICAL)
        mySizes.fillRows(index0, index1 + 1, null);
      else
        assert false : orientation;
    }

    public void insertLines(InlineLayout.Orientation orientation, int index0, int index1) {
      super.insertLines(orientation, index0, index1);
      int length = index1 - index0 + 1;
      assert length > 0 : index0 + " " + index1;
      if (orientation == InlineLayout.HORISONTAL)
        mySizes.insertColumns(index0, length, null);
      else if (orientation == InlineLayout.VERTICAL)
        mySizes.insertRows(index0, length);
      else
        assert false : orientation;
    }

    public void removeLines(InlineLayout.Orientation orientation, int index0, int index1) {
      super.removeLines(orientation, index0, index1);
      assert index0 <= index1 : index0 + " " + index1;
      if (orientation == InlineLayout.HORISONTAL)
        mySizes.removeColumns(index0, index1 + 1);
      else if (orientation == InlineLayout.VERTICAL)
        mySizes.removeRows(index0, index1 + 1);
      else
        assert false : orientation;
    }
  }


  private class CellSizeLayout extends CellSizes {

    public Dimension getCellSize(InlineLayout.Orientation orientation, int pri, int sec) {
      validateGrid(InlineLayout.HORISONTAL);
      validateGrid(InlineLayout.VERTICAL);
      return new Dimension(getLineWidth(orientation, pri), getLineWidth(orientation.getOpposite(), sec));
    }

    private void validateGrid(InlineLayout.Orientation orientation) {
      int total = orientation.getAlong(LightTableLayout.this.getSize());
      CellSizes pref = LightTableLayout.this.getCachedSizes(InlineLayout.PREF_SIZE);
      int prefWidth = pref.getTotal(orientation);
      CellSizes limit;
      if (total <= prefWidth) {
        limit = LightTableLayout.this.getCachedSizes(InlineLayout.MIN_SIZE);
      } else {
        limit = LightTableLayout.this.getCachedSizes(InlineLayout.MAX_SIZE);
      }
      assert false : "Not done yet";
    }
  }


  private class MyListListener implements AListModel.Listener {
    private final InlineLayout.Orientation myOrientation;
    private Detach myDetach = Detach.NOTHING;

    public MyListListener(InlineLayout.Orientation orientation) {
      myOrientation = orientation;
    }

    public void attach(@NotNull AListModel<?> model) {
      myDetach.detach();
      myDetach = model.addListener(this);
    }

    public void onInsert(int index, int length) {
      if (length == 0) {
        assert false;
        return;
      }
      for (CellSizes cellSizes : mySizeCache)
        cellSizes.insertLines(myOrientation, index, index + length - 1);
      repaintAffected(index);
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
      if (length == 0) {
        assert false;
        return;
      }
      for (CellSizes cellSizes : mySizeCache)
        cellSizes.removeLines(myOrientation, index, index + length - 1);
      repaintAffected(index);
    }

    public void onListRearranged(AListModel.AListEvent event) {
      invalidateCaches(event.getLowAffectedIndex(), event.getHighAffectedIndex());
    }

    private void invalidateCaches(int low, int high) {
      for (CellSizes cellSizes : mySizeCache)
        cellSizes.invalidate(myOrientation, low, high);
      repaintAffected(low);
    }

    private void repaintAffected(int low) {
      CellSizes layout = getCachedSizes(null);
      int firstPixel = layout.getLineStart(myOrientation, low);
      Dimension tableSize = LightTableLayout.this.getSize();
      int along = myOrientation.getAlong(tableSize);
      int across = myOrientation.getAcross(tableSize);
      int alongSize = along - firstPixel;
      if (alongSize < 0)
        return;
      repaint(myOrientation.createRectange(firstPixel, 0, alongSize, across));
    }

    public void onItemsUpdated(AListModel.UpdateEvent event) {
      invalidateCaches(event.getLowAffectedIndex(), event.getHighAffectedIndex());
    }
  }


  private class MyCellState extends ComponentCellState {
    private final int myColumn;
    private final int myRow;

    public MyCellState(int column, int row) {
      super(LightTableLayout.this);
      myColumn = column;
      myRow = row;
    }

    public Color getBackground() {
      return getBackground(true);
    }

    @Override
    public Color getBackground(boolean opaque) {
      return getDefaultBackground();
    }

    public Color getSelectionBackground() {
      return getDefaultBackground();
    }

    @Nullable
    public Border getBorder() {
      return null;
    }

    @NotNull
    public Color getForeground() {
      return getDefaultForeground();
    }

    public boolean isExpanded() {
      return false;
    }

    public boolean isFocused() {
      return ListSpeedSearch.isFocusOwner(LightTableLayout.this);
    }

    public boolean isLeaf() {
      return false;
    }

    public boolean isSelected() {
      return false;
    }

    public int getCellColumn() {
      return myColumn;
    }

    public int getCellRow() {
      return myRow;
    }

    public int getComponentCellWidth() {
      return 0;
    }

    public int getComponentCellHeight() {
      return 0;
    }
  }
}
