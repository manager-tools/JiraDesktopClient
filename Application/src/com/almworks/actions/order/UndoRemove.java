package com.almworks.actions.order;

import com.almworks.api.application.order.ReorderItem;
import com.almworks.util.advmodel.OrderListModel;

import java.util.Arrays;

/**
 * @author dyoma
 */
class UndoRemove extends UndoEntry {
  private ReorderItem[] myOrder;

  public UndoRemove(ReorderItem[] order, int[] selection) {
    super(selection);
    myOrder = order;
  }

  protected void perform(OrderListModel<ReorderItem> model) {
    ReorderItem[] order = myOrder;
    myOrder = model.toList().toArray(new ReorderItem[model.getSize()]);
    model.setElements(Arrays.asList(order));
    model.updateAll();
  }
}
