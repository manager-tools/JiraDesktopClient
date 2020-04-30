package com.almworks.api.application.order;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.items.sync.DBDrain;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.TableColumnAccessor;

import java.util.Comparator;
import java.util.List;

/**
 * @author dyoma
 */
public interface Order {
  Comparator<Order> NAME_COMPARATOR = new OrderComparator();
  CanvasRenderer<Order> RENDERER = new OrderRenderer();

  String getId();

  String getDisplayName();

  TableColumnAccessor<ReorderItem, ?> getColumn();

  boolean canOrder(ItemWrapper item);

  /**
   * Update order values in the list so that the list becomes ordered. Indices contain positions in the list of
   * items already changed by the user.
   *
   * @param list new order of items
   * @param indices moved items's positions
   */
  void updateOrder(List<ReorderItem> list, int[] indices);

  Comparator<LoadedItem> getComparator();

  void updateItems(DBDrain writer, List<ReorderItem> items);

  public static class OrderComparator implements Comparator<Order> {
    public int compare(Order o1, Order o2) {
      if (o1 == null)
        return o2 == null ? 0 : -1;
      if (o2 == null)
        return 1;
      return o1.getDisplayName().compareTo(o2.getDisplayName());
    }
  }

  public static class OrderRenderer implements CanvasRenderer<Order> {
    public void renderStateOn(CellState state, Canvas canvas, Order item) {
      canvas.appendText(item.getDisplayName());
    }
  }
}
