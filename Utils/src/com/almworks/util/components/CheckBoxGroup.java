package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.IntArray;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author dyoma
 */
public class CheckBoxGroup<T> implements ComponentsListController.ListAccessor<T>, AListModel.Listener<T> {
  private static final ComponentProperty<Object> ITEM = ComponentProperty.createProperty("item");
  private static final ComponentProperty<Detach> DETACH = ComponentProperty.createProperty("detach");
  private final ComponentsListController<T> myController;
  private final SegmentedListModel<T> myModel = SegmentedListModel.create(AListModel.EMPTY);
  private final SelectionAccessor<T> mySelection = new MySelectionAccessor();

  public CheckBoxGroup() {
    myController = new ComponentsListController<T>(this);
    myModel.addListener(this);
  }

  public void setModel(AListModel<T> model) {
    myModel.setSegment(0, model);
  }

  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelection;
  }

  public void onInsert(int index, int length) {
    myController.insertComponents(index, length);
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent<T> event) {
    myController.removeComponents(event);
  }

  public void onListRearranged(AListModel.AListEvent event) {
    myController.rearrangeComponents(event);
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
    myController.updateComponents(event);
  }

  public AbstractButton doCreateButton(T t) {
    JCheckBox button = new JCheckBox();
    Detach detach = UIUtil.addSelectedListener(button, new ChangeListener() {
      public void onChange() {
        mySelection.fireSelectionChanged();
      }
    });
    DETACH.putClientValue(button, detach);
    return button;
  }

  public void onButtonRemoved(AbstractButton button) {
    DETACH.getClientValue(button).detach();
  }

  public T getItemAt(int index) {
    return myModel.getAt(index);
  }

  public void onButtonUpdated(AbstractButton button, T t) {
    ITEM.putClientValue(button, t);
    if (mySelection.isSelected(t))
      mySelection.fireSelectedItemsChanged();
  }

  public static <T> CheckBoxGroup<T> create() {
    return new CheckBoxGroup<T>();
  }

  public JComponent getPanel() {
    return myController.getPanel();
  }

  public void setOpaque(boolean opaque) {
    myController.setOpaque(opaque);
  }

  public void setRenderer(CanvasRenderer<T> renderer) {
    myController.setRenderer(renderer);
  }

  private class MySelectionAccessor extends SelectionAccessor<T> {
    public T getSelection() {
      AbstractButton button = getSelectedButton();
      return button != null ? (T) ITEM.getClientValue(button) : null;
    }

    public boolean hasSelection() {
      return getSelectedIndex() != -1;
    }

    @NotNull
    public List<T> getSelectedItems() {
      int[] indecies = getSelectedIndexes();
      List<T> result = Collections15.arrayList(indecies.length);
      for (int index : indecies) {
        result.add(myModel.getAt(index));
      }
      return result;
    }


    public T getFirstSelectedItem() {
      return null;
    }

    public T getLastSelectedItem() {
      return null;
    }

    @NotNull
    public int[] getSelectedIndexes() {
      IntArray result = new IntArray();
      for (int i = 0; i < myController.getComponentCount(); i++) {
        AbstractButton component = myController.getComponent(i);
        if (component.isSelected())
          result.add(i);
      }
      return result.toNativeArray();
    }

    public boolean setSelected(T t) {
      if (isSelected(t))
        return true;
      clearSelection();
      return addSelectionImpl(t);
    }

    public void setSelectedIndex(int index) {
      if (isSelectedAt(index))
        return;
      clearSelection();
      myController.getComponent(index).setSelected(true);
    }

    protected int getElementCount() {
      return myModel.getSize();
    }

    protected T getElement(int index) {
      return myModel.getAt(index);
    }

    public boolean isSelected(T t) {
      AbstractButton button = myController.findButton(t);
      return button != null && button.isSelected();
    }

    public void selectAll() {
      selectAll(true);
    }

    public void clearSelection() {
      selectAll(false);
    }

    public void invertSelection() {
      for (int i = 0; i < myController.getComponentCount(); i++) {
        AbstractButton component = myController.getComponent(i);
        component.setSelected(!component.isSelected());
      }
    }

    public void addSelectedRange(int first, int last) {
      setSelectedRange(first, last, true);
    }

    @Override
    public void removeSelectedRange(int first, int last) {
      setSelectedRange(first, last, false);
    }

    public void setSelectedRange(int first, int last, boolean makeSelected) {
      for (int i = first; i <= last; i++) {
        AbstractButton component = myController.getComponent(i);
        component.setSelected(makeSelected);
      }
    }

    public int getSelectedIndex() {
      for (int i = 0; i < myController.getComponentCount(); i++) {
        AbstractButton component = myController.getComponent(i);
        if (component.isSelected())
          return i;
      }
      return -1;
    }

    public boolean ensureSelectionExists() {
      if (hasSelection())
        return true;
      if (myController.getComponentCount() <= 0)
        return false;
      myController.getComponent(0).setSelected(true);
      return true;
    }

    public void addSelection(T t) {
      addSelectionImpl(t);
    }

    public boolean isAllSelected() {
      for (int i = 0; i < myController.getComponentCount(); i++) {
        AbstractButton component = myController.getComponent(i);
        if (!component.isSelected())
          return false;
      }
      return true;
    }

    public boolean isSelectedAt(int index) {
      return myController.getComponent(index).isSelected();
    }

    public void addSelectionIndex(int index) {
      myController.getComponent(index).setSelected(true);
    }

    public void removeSelectionAt(int index) {
      myController.getComponent(index).setSelected(false);
    }

    public void removeSelection(T t) {
      AbstractButton button = myController.findButton(t);
      if (button != null)
        button.setSelected(false);
    }

    @Nullable
    private AbstractButton getSelectedButton() {
      int index = getSelectedIndex();
      return index != -1 ? myController.getComponent(index) : null;
    }

    private void selectAll(boolean select) {
      for (int i = 0; i < myController.getComponentCount(); i++) {
        AbstractButton component = myController.getComponent(i);
        component.setSelected(select);
      }
    }

    private boolean addSelectionImpl(T t) {
      AbstractButton button = myController.findButton(t);
      if (button != null) {
        button.setSelected(true);
        return true;
      }
      return false;
    }
  }
}
