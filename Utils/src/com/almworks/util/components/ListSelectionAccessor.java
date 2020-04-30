package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.IntArray;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Collection;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ListSelectionAccessor<T> extends SelectionAccessor<T> implements ListSelectionListener {
  private final ListSelectionModel mySelection;
  private final AListModel<? extends T> myItems;
  private int myLastLeadIndex = -1;

  public ListSelectionAccessor(FlatCollectionComponent<T> component) {
    this(component.getSelectionModel(), component.getCollectionModel());
  }

  ListSelectionAccessor(ListSelectionModel selection, AListModel<? extends T> items) {
    assert selection != null;
    assert items != null;
    mySelection = selection;
    myItems = items;
    mySelection.addListSelectionListener(this);
    myItems.addListener(new AListModel.Adapter() {
      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
          if (mySelection.isSelectedIndex(i)) {
            fireSelectionChanged();
            return;
          }
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        int[] indices = getSelectedIndexes();
        boolean selectedChanged = false;
        for (int i = 0; i < indices.length; i++) {
          int index = indices[i];
          if (event.isUpdated(index)) {
            selectedChanged = true;
            break;
          }
        }
        if (selectedChanged)
          fireSelectedItemsChanged();
      }
    });
  }

  public T getSelection() {
    Threads.assertAWTThread();
    int index = getSelectedIndex();
    return index >= 0 && index < getModelSize() ? getCollectionItemAt(index) : null;
  }

  private T getCollectionItemAt(int index) {
    return myItems.getAt(index);
  }

  private int getModelSize() {
    return myItems.getSize();
  }

  public boolean hasSelection() {
    Threads.assertAWTThread();
    return getSelectedIndex() >= 0;
  }

  protected int getElementCount() {
    return myItems.getSize();
  }

  protected T getElement(int index) {
    return myItems.getAt(index);
  }

  @NotNull
  public List<T> getSelectedItems() {
    Threads.assertAWTThread();
    int size = getModelSize();
    if (size == 0)
      return Collections15.emptyList();
    int min = mySelection.getMinSelectionIndex();
    if (min < 0)
      return Collections15.emptyList();
    int max = mySelection.getMaxSelectionIndex();
    if (max < 0)
      return Collections15.emptyList();
    if (max < min) {
      int t = min;
      min = max;
      max = t;
    }
    min = Math.min(min, size - 1);
    max = Math.min(max, size - 1);
    assert max >= min : min + " " + max + " " + mySelection;
    List<T> result = Collections15.arrayList(max - min + 1);
    for (int i = min; i <= max; i++)
      if (mySelection.isSelectedIndex(i))
        result.add(getCollectionItemAt(i));
    return result;
  }


  public T getFirstSelectedItem() {
    int index = mySelection.getAnchorSelectionIndex();
    if (index >= 0 && index < getModelSize()) {
      if (mySelection.isSelectedIndex(index)) {
        return getCollectionItemAt(index);
      }
    }
    return null;
  }

  public T getLastSelectedItem() {
    int index = mySelection.getLeadSelectionIndex();
    if (index >= 0 && index < getModelSize()) {
      if (mySelection.isSelectedIndex(index)) {
        return getCollectionItemAt(index);
      }
    }
    return null;
  }

  @NotNull
  public int[] getSelectedIndexes() {
    Threads.assertAWTThread();
    IntArray indices = UIUtil.getSelectedIndices(mySelection);
    int modelSize = getModelSize();
    int i = 0;
    while (i < indices.size()) {
      int index = indices.get(i);
      if (index >= modelSize) {
        if (i == 0)
          return Const.EMPTY_INTS;
        else {
          indices.removeRange(i, indices.size());
          return indices.toNativeArray();
        }
      }
      i++;
    }
    return indices.toNativeArray();
  }

  public boolean setSelected(T item) {
    Threads.assertAWTThread();
    int index = myItems.indexOf(item);
    if (index != -1) {
      setSelectedIndex(index);
      return true;
    } else {
      clearSelection();
      return false;
    }
  }

  public int indexOf(T item) {
    return myItems.indexOf(item);
  }

  @Override
  protected void priSetSelected(Collection<? extends T> items) {
    IntArray indexes = new IntArray();
    for (T item : items) {
      int index = indexOf(item);
      if (index >= 0) indexes.add(index);
    }
    indexes.sort();
    indexes.removeSubsequentDuplicates();
    setSelectedIndexes(indexes);
  }

  public void setSelectedIndex(int index) {
    Threads.assertAWTThread();
    if (index < 0 || index > myItems.getSize()) clearSelection();
    else mySelection.setSelectionInterval(index, index);
  }

  public boolean isSelected(T item) {
    int index = myItems.indexOf(item);
    return index != -1 ? mySelection.isSelectedIndex(index) : false;
  }

  public boolean isSelectedAt(int index) {
    assert index >= 0 : index;
    return mySelection.isSelectedIndex(index);
  }

  public void addSelectionIndex(int index) {
    addSelectedRangeImpl(index, index);
  }

  public void removeSelectionAt(int index) {
    removeSelectedRange(index, index);
  }

  public void removeSelection(T element) {
    Threads.assertAWTThread();
    int index = myItems.indexOf(element);
    removeSelectionAt(index);
  }

  public void valueChanged(ListSelectionEvent e) {
    Threads.assertAWTThread();
    if (e != null && e.getValueIsAdjusting())
      return;
    fireSelectionChanged();
  }

  public void selectAll() {
    Threads.assertAWTThread();
    addSelectedRangeImpl(0, myItems.getSize() - 1);
  }

  public void clearSelection() {
    Threads.assertAWTThread();
    if (mySelection.getMinSelectionIndex() < 0) return;
    mySelection.clearSelection();
  }

  public void invertSelection() {
    Threads.assertAWTThread();
    int min = mySelection.getMinSelectionIndex();
    int max = mySelection.getMaxSelectionIndex();
    if (min == -1) {
      selectAll();
      return;
    }
    int[] clear = getSelectedIndexes();
    IntArray select = new IntArray(myItems.getSize() - clear.length);
    for (int i = 0; i < myItems.getSize(); i++) {
      if (!mySelection.isSelectedIndex(i)) select.add(i);
    }
    changeSelectionAt(IntArray.sortedNoDuplicates(clear), false);
    changeSelectionAt(select, true);
  }

  public void addSelectedRange(int first, int last) {
    Threads.assertAWTThread();
    addSelectedRangeImpl(first, last);
  }

  @Override
  public void removeSelectedRange(int first, int last) {
    Threads.assertAWTThread();
    removeSelectedRangeImpl(first, last);
  }

  private void removeSelectedRangeImpl(int first, int last) {
    mySelection.removeSelectionInterval(first, last);
  }

  private void addSelectedRangeImpl(int first, int last) {
    mySelection.addSelectionInterval(first, last);
  }

  /**
   * Gets one index from the selection, according to logic similar to Outlook Express.
   */
  public int getSelectedIndex() {
    Threads.assertAWTThread();
    int min = mySelection.getMinSelectionIndex();
    if (min == -1) {
      // selection is empty
      return -1;
    }
    int index;
    int size = myItems.getSize();
    // try lead index. NB: lead item may be not selected!
    index = mySelection.getLeadSelectionIndex();
    if (index >= 0 && index < size && mySelection.isSelectedIndex(index)) {
      myLastLeadIndex = index;
      return index;
    }
    // try last lead index. may be not selected as well
    index = myLastLeadIndex;
    if (index >= 0 && index < size && mySelection.isSelectedIndex(index)) {
      return index;
    }
    // use min selection by default
    return min < size ? min : -1;
  }

  public boolean ensureSelectionExists() {
    Threads.assertAWTThread();
    if (hasSelection())
      return true;
    if (myItems.getSize() == 0)
      return false;
    addSelectedRange(0, 0);
    return true;
  }

  public void addSelection(T item) {
    Threads.assertAWTThread();
    int index = myItems.indexOf(item);
    if (index >= 0)
      addSelectedRangeImpl(index, index);
  }

  public boolean isAllSelected() {
    Threads.assertAWTThread();
    for (int i = 0; i < myItems.getSize() - 1; i++)
      if (!mySelection.isSelectedIndex(i))
        return false;
    return true;
  }
}
