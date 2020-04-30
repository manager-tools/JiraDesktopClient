package com.almworks.util.ui.widgets;

import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

/**
 * Represents cell. Provides access to cell tree
 */
public interface HostCell extends CellContext {
  Comparator<HostCell> ID_ORDER = new Comparator<HostCell>() {
    public int compare(HostCell o1, HostCell o2) {
      return Util.compareInts(getId(o1), getId(o2));
    }

    private int getId(HostCell cell) {
      return cell != null ? cell.getId() : -1;
    }
  };

  /**
   * @return the cell identifier
   */
  int getId();

  /**
   * @return ids of all active children
   */
  int[] getChildrenCellIds();

  /**
   * Modified children ids to new Ids.
   * @param newIds
   * @throws IllegalArgumentException if newIds aren't unique or length of newId isn't equals to current active childnre count
   */
  void remapChildrenIds(int[] newIds);
  /**
   * @return parent cell
   */
  HostCell getParent();

  /**
   * @return the widget associated with the cell
   */
  Widget<?> getWidget();

  /**
   * Associate component with the cell
   */
  void setLiveComponent(JComponent component);

  /**
   * @param childId
   * @param forward
   * @return next child in given order.
   */
  @Nullable
  HostCell getNextChild(int childId, boolean forward);

  /**
   * @return number of ancestors
   */
  int getTreeDepth();

  /**
   *
   * @param widget the widget associated with this cell. Not actually required, just for type and other safety
   * @param <T> value type
   * @return the value at this cell.
   */
  @Nullable
  <T> T restoreValue(Widget<? extends T> widget);

  /**
   * @return nearest ancestor accosiated with the widget.
   */
  @Nullable
  HostCell getAncestor(Widget<?> ancestorWidget);

  List<HostCell> getChildrenList();

  /**
   * Out of the box support for cell lifecycle. The life is managed by widgets implementation. The life starts at first
   * call to the method (if cell is already active), ends when cell is deactivated. <br>
   * Note for adding listeners for active lifespan. The listener notification may reach listener when cell is already
   * deactivated. Check that cell is still active if notification processing requires cell data.
   * @return lifespan of current active cell or ended lifespan if cell isnt active right now
   */
  @NotNull
  Lifespan getActiveLife();

  enum  Purpose {
    ROOT,
    FOCUSED,
    VISIBLE,
    MOUSE_HOVER
  }
}
