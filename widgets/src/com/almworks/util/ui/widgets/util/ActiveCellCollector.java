package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.CellActivate;
import com.almworks.util.ui.widgets.HostCell;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * Utility implementation of {@link com.almworks.util.ui.widgets.CellActivate} interface. This implementation tracks
 * all cells where the widget is activated and allows to control them all with single calls.
 */
public final class ActiveCellCollector implements CellActivate {
  private final List<HostCell> myCells = Collections15.arrayList();

  @Override
  public void activate(@NotNull HostCell cell) {
    myCells.add(cell);
  }

  @Override
  public void deactivate(@NotNull HostCell cell, JComponent liveComponent) {
    myCells.remove(cell);
  }

  /**
   * @return number of cells activated for the widget
   */
  public int size() {
    return myCells.size();
  }

  /**
   * Request to repaint all active cells
   */
  public void repaint() {
    if (myCells.isEmpty()) return;
    for (HostCell cell : myCells) cell.repaint();
  }

  /**
   * Request to revalidate all active cells
   */
  public void revalidate() {
    for (HostCell cell : myCells) cell.invalidate();
  }

  /**
   * Delete children with given id from all active cells
   * @param id
   */
  public void deleteChild(int id) {
    if (myCells.isEmpty()) return;
    for (HostCell cell : myCells) {
      HostCell child = cell.findChild(id);
      if (child != null) child.deleteAll();
    }
  }

  public HostCell getActiveCell(int index) {
    HostCell cell = myCells.get(index);
    return cell != null && cell.isActive() ? cell : null;
  }

  public List<HostCell> getCells() {
    return Collections.unmodifiableList(myCells);
  }
}
