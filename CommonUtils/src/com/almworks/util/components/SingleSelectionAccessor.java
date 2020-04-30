package com.almworks.util.components;

import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SingleSelectionAccessor<T> extends SelectionAccessor<T> {
  private final T mySelection;

  public SingleSelectionAccessor(T selection) {
    assert selection != null;
    mySelection = selection;
  }

  public T getSelection() {
    return mySelection;
  }

  public boolean hasSelection() {
    return true;
  }

  @NotNull
  public List<T> getSelectedItems() {
    return Collections.singletonList(mySelection);
  }

  public T getFirstSelectedItem() {
    return mySelection;
  }

  public T getLastSelectedItem() {
    return mySelection;
  }

  @NotNull
  public int[] getSelectedIndexes() {
    return Const.EMPTY_INTS;
  }

  public boolean setSelected(T item) {
    assert false : item;
    return false;
  }

  public void setSelectedIndex(int index) {
    assert false : index;
  }

  public boolean isSelected(T item) {
    return Util.equals(item, mySelection);
  }

  public void selectAll() {
    assert false;
  }

  public void clearSelection() {
    assert false;
  }

  public void invertSelection() {
    assert false;
  }

  public void addSelectedRange(int first, int last) {
    assert false : first + " " + last;
  }

  @Override
  public void removeSelectedRange(int first, int last) {
    assert false : first + " " + last;
  }

  public int getSelectedIndex() {
    return 0;
  }

  public boolean ensureSelectionExists() {
    return true;
  }

  public void addSelection(T item) {
    assert false : item;
  }

  public boolean isAllSelected() {
    return true;
  }

  public boolean isSelectedAt(int index) {
    return index == 0;
  }

  public void addSelectionIndex(int index) {
    assert false : index;
  }

  public void removeSelectionAt(int index) {
    assert false : index;
  }

  public void removeSelection(T element) {
    assert false : element;
  }

  protected int getElementCount() {
    return 1;
  }

  protected T getElement(int index) {
    if (index == 0)
      return mySelection;
    assert false : index;
    return null;
  }
}
