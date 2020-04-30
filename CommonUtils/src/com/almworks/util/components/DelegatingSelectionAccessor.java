package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DelegatingSelectionAccessor<T> extends SelectionAccessor<T> {
  private final SelectionAccessor<T> myDelegate;

  public DelegatingSelectionAccessor(SelectionAccessor<T> delegate) {
    myDelegate = delegate;
  }

  public SelectionAccessor<T> getDelegate() {
    return myDelegate;
  }

  @Override
  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    myDelegate.addAWTChangeListener(life, listener);
  }

  @Override
  public Detach addAWTChangeListener(ChangeListener listener) {
    return myDelegate.addAWTChangeListener(listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    myDelegate.addChangeListener(life, gate, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myDelegate.addChangeListener(life, listener);
  }

  public Detach addListener(Listener<? super T> listener) {
    return myDelegate.addListener(listener);
  }

  @Override
  public Detach addSelectedItemsListener(ChangeListener listener) {
    return myDelegate.addSelectedItemsListener(listener);
  }

  @Override
  public void addSelectedRange(int first, int last) {
    myDelegate.addSelectedRange(first, last);
  }

  public void addSelection(T item) {
    myDelegate.addSelection(item);
  }

  @Override
  public void addSelectionIndex(int index) {
    myDelegate.addSelectionIndex(index);
  }

  @Override
  @NotNull
  public Condition<T> areSelected() {
    return myDelegate.areSelected();
  }

  @Override
  public void changeSelectionAt(IntArray sortedIndexes, boolean makeSelected) {
    myDelegate.changeSelectionAt(sortedIndexes, makeSelected);
  }

  @Override
  public void clearSelection() {
    myDelegate.clearSelection();
  }

  @Override
  public boolean ensureSelectionExists() {
    return myDelegate.ensureSelectionExists();
  }

  @Override
  public void fireSelectedItemsChanged() {
    myDelegate.fireSelectedItemsChanged();
  }

  @Override
  public void fireSelectionChanged() {
    myDelegate.fireSelectionChanged();
  }

  @Override
  public T getElement(int index) {
    return myDelegate.getElement(index);
  }

  @Override
  public int getElementCount() {
    return myDelegate.getElementCount();
  }

  @Override
  @Nullable
  public T getFirstSelectedItem() {
    return myDelegate.getFirstSelectedItem();
  }

  @Override
  @Nullable
  public T getLastSelectedItem() {
    return myDelegate.getLastSelectedItem();
  }

  @Override
  public int getSelectedCount() {
    return myDelegate.getSelectedCount();
  }

  @Override
  public int getSelectedIndex() {
    return myDelegate.getSelectedIndex();
  }

  @NotNull
  @Override
  public int[] getSelectedIndexes() {
    return myDelegate.getSelectedIndexes();
  }

  @Override
  @NotNull
  public List<T> getSelectedItems() {
    return myDelegate.getSelectedItems();
  }

  @Override
  @Nullable
  public T getSelection() {
    return myDelegate.getSelection();
  }

  @Override
  public boolean hasSelection() {
    return myDelegate.hasSelection();
  }

  @Override
  public void invertSelection() {
    myDelegate.invertSelection();
  }

  @Override
  public boolean isAllSelected() {
    return myDelegate.isAllSelected();
  }

  public boolean isSelected(T item) {
    return myDelegate.isSelected(item);
  }

  @Override
  public boolean isSelectedAt(int index) {
    return myDelegate.isSelectedAt(index);
  }

  public void priSetSelected(Collection<? extends T> items) {
    myDelegate.priSetSelected(items);
  }

  public void removeListener(Listener<? super T> listener) {
    myDelegate.removeListener(listener);
  }

  @Override
  public void removeSelectedRange(int first, int last) {
    myDelegate.removeSelectedRange(first, last);
  }

  public void removeSelection(T element) {
    myDelegate.removeSelection(element);
  }

  @Override
  public void removeSelectionAt(int index) {
    myDelegate.removeSelectionAt(index);
  }

  @Override
  public void selectAll() {
    myDelegate.selectAll();
  }

  public void selectAll(Condition<? super T> condition) {
    myDelegate.selectAll(condition);
  }

  @Override
  public void setEventsInhibited(boolean inhibited) {
    myDelegate.setEventsInhibited(inhibited);
  }

  public boolean setSelected(T item) {
    return myDelegate.setSelected(item);
  }

  @Override
  public void setSelectedIndex(int index) {
    myDelegate.setSelectedIndex(index);
  }

  @Override
  public void setSelectedIndexes(int[] indices) {
    myDelegate.setSelectedIndexes(indices);
  }

  @Override
  public void setSelectedIndexes(IntArray sortedUniquIndexes) {
    myDelegate.setSelectedIndexes(sortedUniquIndexes);
  }

  @Override
  public void updateSelectionAt(IntArray indexes, boolean makeSelected) {
    myDelegate.updateSelectionAt(indexes, makeSelected);
  }
}
