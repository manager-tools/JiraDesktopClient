package com.almworks.util.advmodel;

import com.almworks.util.collections.IntArray;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class ListSelectionModelAdapter <T> implements AListModel.Listener {
  private final ListSelectionModel mySelection;
  private final AListModel<T> myList;
  private final boolean myUpdateAddRemove;

  /**
   * @param updateAddRemove update selection model on elements added or removed.
   *                        See {@link javax.swing.plaf.basic.BasicListUI.ListDataHandler}
   */
  private ListSelectionModelAdapter(AListModel<T> list, ListSelectionModel selection, boolean updateAddRemove) {
    myList = list;
    mySelection = selection;
    myUpdateAddRemove = updateAddRemove;
  }

  public static <T> void createListening(AListModel<T> list, ListSelectionModel selection, boolean updateAddRemove) {
    ListSelectionModelAdapter<T> adapter = new ListSelectionModelAdapter<T>(list, selection, updateAddRemove);
    list.addListener(adapter);
  }

  public Detach listenTo() {
    return myList.addListener(this);
  }

  public void onInsert(int index, int length) {
    if (!myUpdateAddRemove)
      return;
    mySelection.insertIndexInterval(index, length, true);
    mySelection.removeSelectionInterval(index, index + length - 1);
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    if (!myUpdateAddRemove)
      return;
    mySelection.removeIndexInterval(event.getFirstIndex(), event.getLastIndex());
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
  }

  public void onListRearranged(AListModel.AListEvent event) {
    if (mySelection.isSelectionEmpty())
      return;
    int singleSelectionIndex = mySelection.getMinSelectionIndex();
    int newAnchorSelectionIndex = event.getNewIndex(mySelection.getAnchorSelectionIndex());
    try {
      if (mySelection.getMaxSelectionIndex() == singleSelectionIndex) {
        int newIndex = event.getNewIndex(singleSelectionIndex);
        if (newIndex == singleSelectionIndex)
          return;
        mySelection.setSelectionInterval(newIndex, newIndex);
        return;
      }
      mySelection.setValueIsAdjusting(true);
      IntArray current = UIUtil.getSelectedIndices(mySelection);
      if (current.size() == 0) mySelection.clearSelection();
      else {
        mySelection.clearSelection();
        for (int i = 0; i < current.size(); i++) {
          int index = current.get(i);
          int newIndex = event.getNewIndex(index);
          if (newIndex >= 0) {
            mySelection.addSelectionInterval(newIndex, newIndex);
          }
        }
      }
    } finally {
      mySelection.setAnchorSelectionIndex(newAnchorSelectionIndex);
      mySelection.setValueIsAdjusting(false);
    }
  }
}
