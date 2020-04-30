package com.almworks.util.advmodel;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class ListModelAdapter <T> extends AbstractListModel implements AListModel.Listener {
  private final AListModel<T> myModel;

  public ListModelAdapter(AListModel<T> model) {
    myModel = model;
    myModel.addListener(this);
  }

  public int getSize() {
    return myModel.getSize();
  }

  public Object getElementAt(int index) {
    return myModel.getAt(index);
  }

  public void onInsert(int lowIndex, int length) {
    fireIntervalAdded(this, lowIndex, lowIndex + length - 1);
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    int firstIndex = event.getFirstIndex();
    int lastIndex = event.getLastIndex();
    assert index == firstIndex;
    assert index + length - 1 == lastIndex;
//    fireIntervalRemoved(this, firstIndex, lastIndex);
    fireIntervalRemoved(this, index, index + length - 1);
  }

  public void onListRearranged(AListModel.AListEvent event) {
    fireContentsChanged(this, event.getLowAffectedIndex(), event.getHighAffectedIndex());
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
    fireContentsChanged(this, event.getLowAffectedIndex(), event.getHighAffectedIndex());
  }

  public AListModel<T> getModel() {
    return myModel;
  }
}
