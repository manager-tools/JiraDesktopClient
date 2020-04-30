package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.DropHintProvider;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public abstract class TableDropHintProvider implements DropHintProvider<TableDropHint, JTableAdapter<?>> {
  public static final TableDropHintProvider DEFAULT = new TableDropHintProvider() {
    protected TableDropHint createDropHint(TableDropPoint point) {
      return new DefaultTableDropHint(point);
    }

    protected TableDropPoint createDropPoint(ATable<?> table, int row, Point p) {
      return new TableDropPoint(table, table.getCollectionModel().getAt(row), row);
    }
  };

  public static final TableDropHintProvider INSERTION = new TableDropHintProvider() {
    protected TableDropHint createDropHint(TableDropPoint point) {
      return new InsertTableDropHint(point);
    }

    protected TableDropPoint createDropPoint(ATable<?> table, int row, Point p) {
      Rectangle rect = table.getElementRect(row);
      if (rect != null) {
        int center = rect.y + rect.height / 2;
        if (p.y <= center)
          row--;
      } else
        assert false;
      Object element = row >= 0 ? table.getCollectionModel().getAt(row) : null;
      return new TableDropPoint(table, element, row) {
        protected boolean checkModelElement() {
          return getTargetRow() ==-1 || super.checkModelElement();
        }
      };
    }
  };

  public boolean prepareDropHint(JTableAdapter<?> component, Point p, DragContext context, ContextTransfer transfer) {
    ATable<?> table = component.getATable();
    int row = component.rowAtPoint(new Point(1, p.y));
    AListModel<?> model = table.getCollectionModel();
    if (row >= model.getSize())
      row = -1;
    TableDropPoint lastPoint = context.getValue(DndUtil.TABLE_DROP_POINT);
    if (row < 0 && lastPoint == null) {
      return false;
    }
    if (row < 0) {
      context.putValue(DndUtil.TABLE_DROP_POINT, null);
      return true;
    }
    TableDropPoint newPoint = createDropPoint(table, row, p);
    boolean result = !Util.equals(newPoint, lastPoint);
    if (result) {
      context.putValue(DndUtil.TABLE_DROP_POINT, newPoint);
      context.putValue(DndUtil.DROP_ENABLED, Boolean.TRUE);
      try {
        if (!transfer.canImportDataNow(context, component)) {
          disableDrop(context);
          return true;
        }
      } catch (CantPerformException e) {
        disableDrop(context);
        return true;
      }
    }
    return result;
  }

  private void disableDrop(DragContext context) {
    cleanContext(context);
    context.putValue(DndUtil.DROP_ENABLED, Boolean.FALSE);
  }

  protected abstract TableDropPoint createDropPoint(ATable<?> table, int row, Point p);

  public TableDropHint createDropHint(JTableAdapter<?> component, DragContext context) {
    TableDropPoint point = context.getValue(DndUtil.TABLE_DROP_POINT);
    return point == null || !point.isValid() ? null : createDropHint(point);
  }

  protected abstract TableDropHint createDropHint(TableDropPoint point);

  public void cleanContext(DragContext context) {
    if (context != null)
      context.putValue(DndUtil.TABLE_DROP_POINT, null);
  }

  private static class DefaultTableDropHint extends TableDropHint {
    public DefaultTableDropHint(TableDropPoint dropPoint) {
      super(dropPoint);
    }

    public void paint(Graphics g, JTable table) {
      int row = getDropPoint().getTargetRow();
      if (isNothingToRepaint(table, row))
        return;
      Rectangle bounds = table.getCellRect(row, 0, true);
      bounds.x = 0;
      bounds.width = table.getWidth();
      if (!g.getClipBounds().intersects(bounds))
        return;

      Graphics2D gg = (Graphics2D) g.create();
      try {
        Color c = GlobalColors.DRAG_AND_DROP_COLOR;
        gg.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0x44));
        gg.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        gg.setColor(c);
        gg.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
      } finally {
        gg.dispose();
      }
    }

    public void repaint(JTable table) {
      if (!isValid())
        return;
      int row = getDropPoint().getTargetRow();
      if (isNothingToRepaint(table, row))
        return;
      Rectangle r = table.getCellRect(row, 0, true);
      if (r != null) {
        table.repaint(0, r.y, table.getWidth(), r.height);
      }
    }

    private boolean isNothingToRepaint(JTable table, int row) {
      return row < 0 || row >= table.getRowCount() || table.getColumnCount() <= 0;
    }
  }

  private static class InsertTableDropHint extends TableDropHint {
    private static final Rectangle EMPTY_RECTANGLE = new Rectangle(0, 0 , 0, 0);

    public InsertTableDropHint(TableDropPoint dropPoint) {
      super(dropPoint);
    }

    public void paint(Graphics g, JTable table) {
      int firstRow = getDropPoint().getTargetRow();
      int rowCount = table.getRowCount();
      if (isNothingToRepaint(table, firstRow, rowCount))
        return;
      int y;
      if (firstRow == -1)
        y = table.getCellRect(0, 0, true).y;
      else {
        assert firstRow >= 0;
        Rectangle rect = table.getCellRect(firstRow, 0, true);
        y = rect.y + rect.height;
      }
      Rectangle bounds = new Rectangle(0, y - 1, table.getWidth(), 2);
      if (!g.getClipBounds().intersects(bounds))
        return;
      Graphics graphics = g.create();
      try {
        graphics.setColor(GlobalColors.DRAG_AND_DROP_COLOR);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
      } finally{
        graphics.dispose();
      }
    }

    public void repaint(JTable table) {
      if (!isValid())
        return;
      int firstRow = getDropPoint().getTargetRow();
      int rowCount = table.getRowCount();
      if (isNothingToRepaint(table, firstRow, rowCount))
        return;
      Rectangle cell1 = firstRow >= 0 ? table.getCellRect(firstRow, 0, true) : EMPTY_RECTANGLE;
      Rectangle cell2 = firstRow + 1 < rowCount ? table.getCellRect(firstRow + 1, 0, true) : EMPTY_RECTANGLE;
      int y = cell1.height > 0 ? cell1.y : cell2.y;
      table.repaint(0, y, table.getWidth(), cell1.height + cell2.height);
    }

    private boolean isNothingToRepaint(JTable table, int firstRow, int rowCount) {
      return firstRow < -1 || firstRow >= rowCount || table.getColumnCount() <= 0;
    }
  }
}
