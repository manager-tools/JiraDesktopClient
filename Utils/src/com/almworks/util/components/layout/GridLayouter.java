package com.almworks.util.components.layout;

import org.almworks.util.Collections15;

import java.awt.*;
import java.util.List;

/**
 * @author dyoma
 */
public class GridLayouter {
  private final List<Column> myColumns = Collections15.arrayList();
  private final Dimension myGap = new Dimension();
  private Integer myDefaultRowHeight = null;

  public Column createColumn(List<? extends Component> components) {
    Column column = createColumn();
    column.getComponents().addAllPrefSize(components);
    return column;
  }

  public Column createColumn() {
    Column column = new Column();
    myColumns.add(column);
    return column;
  }

  public Column getColumn(int column) {
    return myColumns.get(column);
  }

  public void setRowHeightToMax() {
    int max = -1;
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      max = Math.max(max, column.getComponents().getMaxAlong());
    }
    myDefaultRowHeight = max > 0 ? max : null;
  }

  public void layoutOn(ContainerArea area) {
    layoutOn(area, 0, 0, getRowCount());
  }

  public void layoutOn(ContainerArea area, int x, int firstRow, int toRow) {
    for (int i = firstRow; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      column.layout(x, area, this, firstRow, toRow);
      x += column.getWidth() + myGap.width;
    }
  }

  public void distributeUnusedWidth(int width) {
    int totalWidth = getTotalWidth();
    int unused = width - totalWidth;
    if (unused <= 0)
      return;
    List<Column> desirouses = Collections15.arrayList();
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      if (!column.isWidthSet())
        desirouses.add(column);
    }
    int columnsLeft = desirouses.size();
    while (columnsLeft > 0) {
      int columnWidth = (unused + columnsLeft - 1) / columnsLeft;
      assert columnWidth <= unused : "Unused: " + unused + " left: " + columnsLeft;
      desirouses.get(desirouses.size() - columnsLeft).setWidth(columnWidth);
      unused -= columnWidth;
      columnsLeft--;
    }
  }

  public int getRowHeight(int row) {
    if (myDefaultRowHeight != null)
      return myDefaultRowHeight;
    int max = 0;
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      max = Math.max(max, column.getOwnRowHeight(row));
    }
    return max;
  }

  public int getVerticalGap() {
    return myGap.height;
  }

  public int getHorizontalGap() {
    return myGap.width;
  }

  public int getTotalWidth() {
    return getColumnsWidth(0, myColumns.size());
  }

  public int getColumnsWidth(int first, int toColumn) {
    int sum = 0;
    for (int i = first; i < toColumn; i++) {
      Column column = myColumns.get(i);
      sum += column.getWidth();
    }
    int columnCount = toColumn - first;
    if (columnCount < 2)
      return sum;
    return sum + myGap.width * (columnCount - 1);
  }

  public int getTotalHeight() {
    return getRowsHeight(0, getRowCount());
  }

  private int getRowsHeight(int firstRow, int toRow) {
    int sum = firstRow;
    for (int r = firstRow; r < toRow; r++)
      sum += getRowHeight(r);
    if (toRow < 2)
      return sum;
    return sum + myGap.height * (toRow - 1);
  }

  public int getRowCount() {
    int max = 0;
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      max = Math.max(max, column.getRowCount());
    }
    return max;
  }

  public void setPreferedWidth() {
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      if (column.isWidthSet())
        continue;
      column.setWidth(column.getComponents().getAcross());
    }
  }

  public void setMaxPreferedWidth() {
    int max = 0;
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      if (column.isWidthSet())
        continue;
      max = Math.max(max, column.getComponents().getAcross());
    }
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      if (column.isWidthSet())
        continue;
      column.setWidth(max);
    }
  }

  public Dimension getSize() {
    return new Dimension(getTotalWidth(), getTotalHeight());
  }

  public void setGap(int horizontal, int vertical) {
    myGap.width = horizontal;
    myGap.height = vertical;
  }

  public int getColumnCount() {
    return myColumns.size();
  }

  public int getRowStart(int row) {
    int sum = 0;
    for (int i = 0; i < row; i++)
      sum += getRowHeight(i) + getVerticalGap();
    return sum;
  }

  public int getColumnStart(int columnIndex) {
    int sum = 0;
    for (int i = 0; i < columnIndex; i++) {
      Column column = myColumns.get(i);
      sum += column.getWidth() + getHorizontalGap();
    }
    return sum;
  }

  public Rectangle getRectangle(int column, int row, int columnCount, int rowCount) {
    int x = getColumnStart(column);
    int y = getRowStart(row);
    int width = getColumnsWidth(column, column + columnCount);
    int height = getRowsHeight(row, row + rowCount);
    return new Rectangle(x, y, width, height);
  }

  public void removeRow(int row) {
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      column.removeRow(row);
    }
  }

  public boolean isAnyCellVisible(int row) {
    for (int i = 0; i < myColumns.size(); i++) {
      Column column = myColumns.get(i);
      if (column.isCellVisible(row))
        return true;
    }
    return false;
  }

  public static class Column {
    private final ComponentLine myComponents = ComponentLine.vertical();
    private Integer myWidth = null;

    public ComponentLine getComponents() {
      return myComponents;
    }

    public void setMaxComponentWidth() {
      setWidth(myComponents.getAcross());
    }

    public void setWidth(int width) {
      myWidth = width > 0 ? width : null;
    }

    public int getWidth() {
      return myWidth != null ? myWidth : 0;
    }

    public boolean isWidthSet() {
      return myWidth != null;
    }

    public void layout(int x, ContainerArea area, GridLayouter layout, int firstRow, int toRow) {
      int y = 0;
      for (int i = firstRow; i < Math.min(toRow, myComponents.getComponentCount()); i++) {
        int rowHeight = layout.getRowHeight(i);
        if (myComponents.isVisible(i))
          area.placeChild(myComponents.getComponent(i), x, y, getWidth(), rowHeight);
        y += rowHeight + layout.getVerticalGap();
      }
    }

    public void layout(ContainerArea area, GridLayouter layout) {
      layout(0, area, layout, 0, myComponents.getComponentCount());
    }

    public int getOwnRowHeight(int row) {
      return myComponents.getComponentCount() > row ? myComponents.getAlong(row) : 0;
    }

    public int getRowCount() {
      return myComponents.getComponentCount();
    }

    public void removeRow(int row) {
      if (myComponents.getComponentCount() <= row)
        return;
      myComponents.removeAt(row);
    }

    public boolean isCellVisible(int row) {
      return myComponents.getComponentCount() > row && myComponents.isVisible(row);
    }

    public void expandWidthBy(int additionalWidth) {
      setWidth(getWidth() + additionalWidth);
    }
  }
}
