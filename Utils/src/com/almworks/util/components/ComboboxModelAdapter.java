package com.almworks.util.components;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class ComboboxModelAdapter<T> extends AbstractListModel implements ComboBoxModel {
  private AComboboxModel<T> myModel = AComboboxModel.EMPTY_COMBOBOX;
  private final SelectionListener myListener = new SelectionListener() {
    public void onSelectionChanged() {
      fireContentsChanged(this, -1, -1);
    }

    public void onInsert(int index, int length) {
      assert index >= 0 : index;
      assert myModel.getSize() >= index + length : index + "+" + length + " > " + myModel.getSize();
      fireIntervalAdded(this, index, index + length);
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
      assert index >= 0 : index;
      fireIntervalRemoved(this, event.getFirstIndex(), event.getLastIndex());
    }

    public void onListRearranged(AListModel.AListEvent event) {
      assert event.getLowAffectedIndex() >= 0 : event;
      fireContentsChanged(this, event.getLowAffectedIndex(), event.getHighAffectedIndex());
    }

    public void onItemsUpdated(AListModel.UpdateEvent event) {
      onListRearranged(event);
    }
  };
  private final Lifecycle myModelLife = new Lifecycle();

  public ComboboxModelAdapter(AComboboxModel<T> model) {
    myModel = model != null ? model : AComboboxModel.EMPTY_COMBOBOX;
    myModel.addSelectionListener(myModelLife.lifespan(), myListener);
  }

  @ThreadAWT
  public void setModel(AComboboxModel<T> model) {
    Threads.assertAWTThread();
    myModelLife.cycle();
    int oldSize = myModel.getSize();
    if (oldSize > 0)
      fireIntervalRemoved(this, 0, oldSize);
    myModel = model != null ? model : AComboboxModel.EMPTY_COMBOBOX;
    fireContentsChanged(this, -1, -1);
    int newSize = myModel.getSize();
    if (newSize > 0)
      fireIntervalAdded(this, 0, newSize);
    myModel.addSelectionListener(myModelLife.lifespan(), myListener);
  }

  @Nullable
  public T getSelectedItem() {
    return myModel.getSelectedItem();
  }

  public void setSelectedItem(Object anItem) {
    myModel.setSelectedItem((T) anItem);
  }

  public int getSize() {
    return myModel.getSize();
  }

  @Nullable
  public Object getElementAt(int index) {
    if (index >= 0)
      return myModel.getAt(index);
    assert index == -1 : index;
    return getSelectedItem();
  }

  public AComboboxModel<T> getAModel() {
    return myModel;
  }
}
