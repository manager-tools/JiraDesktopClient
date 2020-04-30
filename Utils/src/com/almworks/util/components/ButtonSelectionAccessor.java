package com.almworks.util.components;

import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class ButtonSelectionAccessor<T> extends SelectionAccessor<T> {
  private final MyButtonGroup myGroup = new MyButtonGroup();
  private final Map<ButtonModel, T> myData = Collections15.hashMap();

  public void addItem(@NotNull AbstractButton button, T data) {
    myData.put(button.getModel(), data);
    myGroup.add(button);
  }

  public T getSelection() {
    ButtonModel model = myGroup.getSelection();
    return myData.get(model);
  }

  public boolean hasSelection() {
    return myGroup.getSelection() != null;
  }

  @NotNull
  public List<T> getSelectedItems() {
    return hasSelection() ? Collections.singletonList(getSelection()) : Collections15.<T>emptyList();
  }

  protected int getElementCount() {
    return myGroup.getButtonCount();
  }

  protected T getElement(int index) {
    return myData.get(myGroup.getModelAt(index));
  }

  public T getFirstSelectedItem() {
    return null;
  }

  public T getLastSelectedItem() {
    return null;
  }

  @NotNull
  public int[] getSelectedIndexes() {
    if (!hasSelection())
      return Const.EMPTY_INTS;
    int selectedIndex = getSelectedIndex();
    assert selectedIndex >= 0;
    return selectedIndex != -1 ? new int[]{selectedIndex} : Const.EMPTY_INTS;
  }

  public boolean setSelected(T item) {
    ButtonModel model = findModel(item);
    if (model == null) {
      clearSelection();
      return false;
    }
    myGroup.setSelected(model, true);
    return true;
  }

  @Nullable
  private ButtonModel findModel(T item) {
    ButtonModel model = null;
    for (Map.Entry<ButtonModel, T> entry : myData.entrySet())
      if (Util.equals(entry.getValue(), item)) {
        model = entry.getKey();
        break;
      }
    return model;
  }

  public void setSelectedIndex(int index) {
    boolean select = true;
    setSelectedAt(index, select);
  }

  private void setSelectedAt(int index, boolean select) {
    AbstractButton button = Containers.getAtOrNull(myGroup.getElements(), index);
    if (button == null) return;
    myGroup.setSelected(button.getModel(), select);
  }

  public boolean isSelected(T item) {
    ButtonModel model = findModel(item);
    return model != null && myGroup.isSelected(model);
  }

  public void selectAll() {
    assert false;
  }

  public void clearSelection() {
    myGroup.setSelected(myGroup.getSelection(), false);
  }

  public void invertSelection() {
    assert false;
  }

  public void addSelectedRange(int first, int last) {
    assert first == last;
    setSelectedIndex(first);
  }

  @Override
  public void removeSelectedRange(int first, int last) {
    assert first == last;
    setSelectedAt(first, false);
  }

  public int getSelectedIndex() {
    ButtonModel model = myGroup.getSelection();
    Enumeration<AbstractButton> all = myGroup.getElements();
    int counter = 0;
    while (all.hasMoreElements()) {
      AbstractButton button = all.nextElement();
      if (model == button.getModel())
        return counter;
      counter++;
    }
    return -1;
  }

  public boolean ensureSelectionExists() {
    if (!hasSelection() && myGroup.getButtonCount() > 0)
      setSelectedIndex(0);
    return hasSelection();
  }

  public void addSelection(T item) {
    setSelected(item);
  }

  public boolean isAllSelected() {
    int count = myGroup.getButtonCount();
    return count == 0 || (hasSelection() && count == 1);
  }

  public boolean isSelectedAt(int index) {
    AbstractButton button = Containers.getAtOrNull(myGroup.getElements(), index);
    return button != null && myGroup.isSelected(button.getModel());
  }

  public void addSelectionIndex(int index) {
    setSelectedIndex(index);
  }

  public void removeSelectionAt(int index) {
    AbstractButton button = Containers.getAtOrNull(myGroup.getElements(), index);
    if (button != null)
      myGroup.setSelected(button.getModel(), false);
  }

  public void removeSelection(T element) {
    ButtonModel model = findModel(element);
    if (model != null)
      myGroup.setSelected(model, false);
  }

  public static <T> ButtonSelectionAccessor<T> create() {
    return new ButtonSelectionAccessor<T>();
  }

  private class MyButtonGroup extends ButtonGroup {
    public void setSelected(ButtonModel m, boolean b) {
      ButtonModel oldSelection = getSelection();
      super.setSelected(m, b);
      boolean newSelection = oldSelection != m;
      if ((b && newSelection) || (!b && !newSelection))
        fireSelectionChanged();
    }

    public ButtonModel getModelAt(int index) {
      return buttons.get(index).getModel();
    }
  }
}
