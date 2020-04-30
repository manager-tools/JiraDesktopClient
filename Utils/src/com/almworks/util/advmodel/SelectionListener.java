package com.almworks.util.advmodel;

public interface SelectionListener extends AListModel.Listener {
  void onSelectionChanged();

  abstract class Adapter implements SelectionListener {
    public void onSelectionChanged() {
    }

    public void onInsert(int index, int length) {
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    }

    public void onListRearranged(AListModel.AListEvent event) {
    }

    public void onItemsUpdated(AListModel.UpdateEvent event) {
    }
  }

  abstract class SelectionOnlyAdapter implements SelectionListener {
    public void onInsert(int index, int length) {
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    }

    public void onListRearranged(AListModel.AListEvent event) {
    }

    public void onItemsUpdated(AListModel.UpdateEvent event) {
    }
  }
}
