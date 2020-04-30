package com.almworks.util.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class RowBorder implements Border {
  private final Border myCellBorder;
  private static final Rectangle CLIP_RECTANGLE = new Rectangle();

  public RowBorder(Border cellBorder) {
    myCellBorder = cellBorder;
  }

  public Insets getBorderInsets(Component c) {
    return myCellBorder.getBorderInsets(c);
  }

  public boolean isBorderOpaque() {
    return myCellBorder.isBorderOpaque();
  }

  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Component t = c;
    int tableX = x;
    while (true) {
      tableX -= t.getX();
      t = t.getParent();
      if (t == null || t instanceof JTable)
        break;
    }
    if (t == null) {
      assert false : c;
      return;
    }

    JTable table = ((JTable) t);
    Insets insets = table.getInsets();
    int borderX = tableX + insets.left;
    TableColumnModel columnModel = table.getColumnModel();
    int i;
    int columnCount = columnModel.getColumnCount();
    for (i = 0; i < columnCount; i++) {
      TableColumn column = columnModel.getColumn(i);
      TableCellRenderer renderer = column.getCellRenderer();
      if (!(renderer instanceof JTableAdapter.TableCellRendererAdapter))
        break;
      CollectionRenderer collectionRenderer = ((JTableAdapter.TableCellRendererAdapter) renderer).getRenderer();
      if (!(collectionRenderer instanceof RowBorderBounding))
        break;
      int v = ((RowBorderBounding) collectionRenderer).getRowBorderX(table, g);
      if (v >= 0) {
        borderX += Math.min(width, v);
        break;
      } else {
        borderX += column.getWidth();
      }
    }
    if (i == columnCount) {
      // no border?
      return;
    }
    int borderWidth = table.getWidth() - insets.right - (borderX - tableX);
    g.getClipBounds(CLIP_RECTANGLE);
    int borderXRight = borderX + borderWidth;
    borderX = Math.max(-1, borderX);
    borderXRight = Math.min(CLIP_RECTANGLE.x + CLIP_RECTANGLE.width + 1, borderXRight);
//    myCellBorder.paintBorder(c, g, borderX, y, borderWidth, height);
    myCellBorder.paintBorder(c, g, borderX, y, borderXRight - borderX, height);
  }
}
