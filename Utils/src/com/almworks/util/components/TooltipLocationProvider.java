package com.almworks.util.components;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public interface TooltipLocationProvider {
  /**
   * Returns a location for immediate tooltip to appear at.
   *
   * @param component  - component that has triggered the tooltip
   * @param mousePoint - point of triggering, in <b>component coordinates</b>.
   * @return point to show the tooltip at, in <b>screen coordinates</b>.
   */
  @NotNull
  Point getTooltipLocation(JComponent component, Point mousePoint);

  TooltipLocationProvider UNDER_COMPONENT = new UnderComponent();
  TooltipLocationProvider UNDER_TABLE_CELL = new UnderTableCell();
  TooltipLocationProvider UNDER_MOUSE = new UnderMouse();


  public static class UnderComponent implements TooltipLocationProvider {
    @NotNull
    public Point getTooltipLocation(JComponent component, Point point) {
      Point location = new Point(point);
      SwingUtilities.convertPointToScreen(location, component);
      int x = location.x;
      component.getLocation(location);
      SwingUtilities.convertPointToScreen(location, component);
      location.x = x;
      location.y = location.y + component.getHeight() + 1;
      return location;
    }
  }


  public static class UnderTableCell implements TooltipLocationProvider {
    @NotNull
    public Point getTooltipLocation(JComponent component, Point mousePoint) {
      if (!(component instanceof JTable)) {
        assert false;
        return mousePoint;
      }
      JTable table = ((JTable) component);
      int row = table.rowAtPoint(mousePoint);
      int column = table.columnAtPoint(mousePoint);
      if (row < 0 || column < 0)
        return new Point(mousePoint.x, mousePoint.y + 16);
      Rectangle cell = table.getCellRect(row, column, true);
      Point p = new Point(mousePoint.x, cell.y + cell.height + 2);
      SwingUtilities.convertPointToScreen(p, component);
      return p;
    }
  }


  public static class UnderMouse implements TooltipLocationProvider {
    @NotNull
    public Point getTooltipLocation(JComponent component, Point mousePoint) {
      Point p = new Point(mousePoint);
      p.y += 16;
      p.x += 16;
      SwingUtilities.convertPointToScreen(p, component);
      return p;
    }
  }
}
