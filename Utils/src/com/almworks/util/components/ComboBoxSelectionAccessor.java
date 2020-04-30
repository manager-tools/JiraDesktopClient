package com.almworks.util.components;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.commons.Condition;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public class ComboBoxSelectionAccessor<T> extends SelectionAccessor<T> {
  private final AComboboxModel<T> myModel;

  public ComboBoxSelectionAccessor(AComboboxModel<T> model) {
    myModel = model;
  }

  public T getSelection() {
    return myModel.getSelectedItem();
  }

  public boolean hasSelection() {
    return true;
  }

  @NotNull
  public List<T> getSelectedItems() {
    return Collections.singletonList(getSelection());
  }


  public T getFirstSelectedItem() {
    return null;
  }

  public T getLastSelectedItem() {
    return null;
  }

  protected int getElementCount() {
    return myModel.getSize();
  }

  protected T getElement(int index) {
    return myModel.getAt(index);
  }

  @NotNull
  public int[] getSelectedIndexes() {
    if (getSelection() == null)
      return new int[] {-1};
    return new int[] {getSelectedIndex()};
  }

  public boolean setSelected(T t) {
    myModel.setSelectedItem(t);
    return true;
  }

  public void setSelectedIndex(int index) {
    if (index == -1)
      return;
    myModel.setSelectedItem(myModel.getAt(index));
  }

  public boolean isSelected(T t) {
    return Util.equals(getSelection(), t);
  }

  public void selectAll() {
  }

  public void clearSelection() {
  }

  public void invertSelection() {
  }

  public void addSelectedRange(int first, int last) {
    setSelectedIndex(first);
  }

  @Override
  public void removeSelectedRange(int first, int last) {
  }

  public int getSelectedIndex() {
    return myModel.detectIndex(Condition.isEqual(getSelection()));
  }

  public boolean ensureSelectionExists() {
    return true;
  }

  public void addSelection(T t) {
  }

  public boolean isAllSelected() {
    return false;
  }

  public boolean isSelectedAt(int index) {
    if (index == -1)
      return getSelectedIndex() == -1;
    return Util.equals(getSelection(), myModel.getAt(index));
  }

  public void addSelectionIndex(int index) {
  }

  public void removeSelectionAt(int index) {
  }

  public void removeSelection(T t) {
  }

  public static <T> ComboBoxSelectionAccessor<T> attach(Lifespan life, AComboboxModel<T> model) {
    final ComboBoxSelectionAccessor<T> accessor = new ComboBoxSelectionAccessor<T>(model);
    model.addSelectionListener(life, new SelectionListener.Adapter() {
      public void onSelectionChanged() {
        accessor.fireSelectionChanged();
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        int index = accessor.getSelectedIndex();
        if (event.isUpdated(index))
          accessor.fireSelectedItemsChanged();
      }
    });
    return accessor;
  }
}
