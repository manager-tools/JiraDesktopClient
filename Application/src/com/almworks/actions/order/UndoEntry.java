package com.almworks.actions.order;

import com.almworks.api.application.order.ReorderItem;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.FlatCollectionComponent;
import com.almworks.util.components.SelectionAccessor;

/**
 * @author dyoma
 */
public abstract class UndoEntry {
  private int[] mySelection;

  protected UndoEntry(int[] selection) {
    mySelection = selection;
  }

  public final void applyTo(FlatCollectionComponent<ReorderItem> component, OrderListModel<ReorderItem> model) {
    int[] selection = mySelection;
    SelectionAccessor<ReorderItem> accessor = component.getSelectionAccessor();
    mySelection = accessor.getSelectedIndexes();
    perform(model);
    accessor.setSelectedIndexes(selection);
  }

  protected abstract void perform(OrderListModel<ReorderItem> model);
}
