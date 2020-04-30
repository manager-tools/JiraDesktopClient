package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

/**
 * @author : Dyoma
 */
public interface AComboboxModel<T> extends AListModel<T> {
  @Nullable
  public T getSelectedItem();

  public void setSelectedItem(T item);

  void addSelectionListener(Lifespan life, SelectionListener listener);

  void addSelectionChangeListener(Lifespan life, ChangeListener listener);

  AComboboxModel EMPTY_COMBOBOX = new EmptyComboBoxModel();


  class EmptyComboBoxModel extends EmptyListModel implements AComboboxModel {
    public Object getSelectedItem() {
      return null;
    }

    public void setSelectedItem(Object item) {
    }

    public void addSelectionListener(Lifespan life, SelectionListener listener) {
    }

    public void addSelectionChangeListener(Lifespan life, ChangeListener listener) {
    }
  }


  static class ListListenerWrapper implements SelectionListener {
    private final Listener myListener;

    public ListListenerWrapper(Listener listener) {
      myListener = listener;
    }

    public void onSelectionChanged() {

    }

    public void onInsert(int index, int length) {
      myListener.onInsert(index, length);
    }

    public void onRemove(int index, int length, RemovedEvent event) {
      myListener.onRemove(index, length, event);
    }

    public void onListRearranged(AListEvent event) {
      myListener.onListRearranged(event);
    }

    public void onItemsUpdated(UpdateEvent event) {
      myListener.onItemsUpdated(event);
    }


    public Listener getListener() {
      return myListener;
    }
  }

  public static class SelectionChangedAdapter extends SelectionListener.SelectionOnlyAdapter {
    private final ChangeListener myListener;

    public SelectionChangedAdapter(ChangeListener listener) {
      myListener = listener;
    }

    public void onSelectionChanged() {
      myListener.onChange();
    }
  }
}
