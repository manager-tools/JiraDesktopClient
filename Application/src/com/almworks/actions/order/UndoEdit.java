package com.almworks.actions.order;

import com.almworks.api.application.order.ReorderItem;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.IntArray;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Arrays;
import java.util.List;

/**
 * @author dyoma
 */
class UndoEdit extends UndoEntry {
  private ReorderItem[] myOrder;
  private int[] myUpdatedIndeces;
  private Object[] myPrevValues;

  private UndoEdit(ReorderItem[] order, int[] updatedIndeces, Object[] prevValues, int[] selection) {
    super(selection);
    myOrder = order;
    myUpdatedIndeces = updatedIndeces;
    myPrevValues = prevValues;
  }

  /**
   * @param prevValues previous values of items of prevOrder
   * @param selection
   */
  public static UndoEdit createEdit(ReorderItem[] prevOrder, Object[] prevValues, int[] selection) {
    IntArray updated = new IntArray();
    List<Object> changedValues = Collections15.arrayList();
    for (int i = 0; i < prevOrder.length; i++) {
      ReorderItem artifact = prevOrder[i];
      Object oldValue = prevValues[i];
      if (!Util.equals(artifact.getNewOrderValue(), oldValue)) {
        updated.add(i);
        changedValues.add(oldValue);
      }
    }
    return new UndoEdit(prevOrder, updated.toNativeArray(), changedValues.toArray(), selection);
  }

  protected void perform(OrderListModel<ReorderItem> model) {
    ReorderItem[] newOrder = model.toList().toArray(new ReorderItem[model.getSize()]);
    IntArray indeces = new IntArray();
    List<Object> values = Collections15.arrayList();
    for (int i = 0; i < myUpdatedIndeces.length; i++) {
      int index = myUpdatedIndeces[i];
      Object prevValue = myPrevValues[i];
      ReorderItem artifact = myOrder[index];
      index = model.indexOf(artifact);
      if (index >= 0) {
        indeces.add(index);
        values.add(artifact.getNewOrderValue());
      }
      artifact.setNewOrderValue(prevValue);
    }
    model.setElements(Arrays.asList(myOrder));
    myOrder = newOrder;
    myUpdatedIndeces = indeces.toNativeArray();
    myPrevValues = values.toArray();
    model.updateAll();
  }
}
