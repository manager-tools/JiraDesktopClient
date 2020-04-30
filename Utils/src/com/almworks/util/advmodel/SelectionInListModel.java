package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author : Dyoma
 */
public class SelectionInListModel <T> extends DelegatingAListModel<T> implements AComboboxModel<T> {
  private final FireEventSupport<SelectionListener> mySelectionListeners =
    FireEventSupport.createSynchronized(SelectionListener.class);
  private final Lifecycle myDataLife = new Lifecycle();
  private AListModel<? extends T> myData = AListModel.EMPTY;
  private T mySelection = null;
  private final FireEventSupport<RemovedElementsListener> myRemoveListeners = FireEventSupport.createSynchronized(RemovedElementsListener.class);

  private SelectionInListModel() {
  }

  @ThreadAWT
  public void setData(@NotNull Lifespan life, @NotNull AListModel<T> data) {
    Threads.assertAWTThread();
    myDataLife.cycle();
    if (life.isEnded()) {
      myData = AListModel.EMPTY;
      return;
    }
    attachData(data);
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        setData(Lifespan.FOREVER, AListModel.EMPTY);
      }
    });
    fireNewDataModel();
  }

  private void fireNewDataModel() {
    int newSize = myData.getSize();
    if (newSize > 0)
      mySelectionListeners.getDispatcher().onInsert(0, newSize);
  }

  @ThreadAWT
  public void setData(@NotNull Collection<T> items) {
    Threads.assertAWTThread();
    setData(Lifespan.FOREVER, FixedListModel.create(items));
  }

  private void attachData(AListModel<? extends T> data) {
    myData = data;
    Lifespan lifespan = myDataLife.lifespan();
    lifespan.add(myData.addListener(mySelectionListeners.getDispatcher()));
    lifespan.add(myData.addRemovedElementListener(myRemoveListeners.getDispatcher()));
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        int oldSize = myData.getSize();
        if (oldSize != 0) {
          AROList.fireRemove(myRemoveListeners.getDispatcher(), mySelectionListeners.getDispatcher(), myData.toList(), 0);
        }
      }
    });
  }

  public T getSelectedItem() {
    return mySelection;
  }

  public void setSelectedItem(T item) {
    mySelection = item;
    mySelectionListeners.getDispatcher().onSelectionChanged();
  }

  public void addSelectionListener(Lifespan life, SelectionListener listener) {
    mySelectionListeners.addStraightListener(life, listener);
  }

  public void addSelectionChangeListener(Lifespan life, ChangeListener listener) {
    if (life.isEnded())
      return;
    addSelectionListener(life, new SelectionChangedAdapter(listener));
  }

  public Detach addListener(Listener<? super T> listener) {
    return mySelectionListeners.addStraightListener(new ListListenerWrapper(listener));
  }

  public void removeFirstListener(final Condition<Listener> condition) {
    mySelectionListeners.removeFirstListener(new Condition<SelectionListener>() {
      public boolean isAccepted(SelectionListener value) {
        if (!(value instanceof ListListenerWrapper)) {
          return false;
        }
        return condition.isAccepted(((ListListenerWrapper) value).getListener());
      }
    });
  }

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return myRemoveListeners.addStraightListener(listener);
  }

  public AListModel<T> getData() {
    return (AListModel<T>) myData;
  }

  protected AListModel<T> getDelegate() {
    return (AListModel<T>) myData;
  }

  public static <T> SelectionInListModel<T> create() {
    return new SelectionInListModel<T>();
  }

  public void synchronizeSelection(Lifespan life, final AComboboxModel<T> model) {
    synchronizeSelection(life, model, this);
  }

  public static <T> void synchronizeSelection(Lifespan life, AComboboxModel<T> from, AComboboxModel<T> to) {
    to.setSelectedItem(from.getSelectedItem());
    SyncSelection<T> syncSelection = new SyncSelection<T>(from, to, null);
    from.addSelectionListener(life, syncSelection);
    to.addSelectionListener(life, new SyncSelection<T>(to, from, syncSelection));
  }

  @NotNull
  public static <T> SelectionInListModel<T> createForever(@NotNull AListModel<T> list, @Nullable T initialSelection) {
    return create(Lifespan.FOREVER, list, initialSelection);
  }

  @NotNull
  public static <T> SelectionInListModel<T> create(@NotNull Lifespan lifespan, @NotNull AListModel<? extends T> list, @Nullable T initialSelection) {
    SelectionInListModel<T> model = new SelectionInListModel<T>();
    model.setSelectedItem(initialSelection);
    if (lifespan.isEnded())
      return model;
    model.attachData(list);
    return model;
  }

  @NotNull
  public static <T> SelectionInListModel<T> create(@NotNull Collection<T> items, @Nullable T initialSelection) {
    SelectionInListModel<T> model = new SelectionInListModel<T>();
    model.setData(items);
    model.setSelectedItem(initialSelection);
    return model;
  }

  private static class SyncSelection<T> extends SelectionListener.Adapter {
    private final AComboboxModel<? extends T> myFrom;
    private final AComboboxModel<T> myTo;
    private final SyncSelection<?> myJoint;
    private boolean myProcessing = false;

    public SyncSelection(AComboboxModel<? extends T> from, AComboboxModel<T> to, SyncSelection<?> joint) {
      myFrom = from;
      myTo = to;
      myJoint = joint != null ? joint : this;
    }

    public void onSelectionChanged() {
      if (isProcessing())
        return;
      setProcessing(true);
      try {
        myTo.setSelectedItem(myFrom.getSelectedItem());
      } finally {
        setProcessing(false);
      }
    }

    public boolean isProcessing() {
      return myJoint.myProcessing;
    }

    public void setProcessing(boolean processing) {
      myJoint.myProcessing = processing;
    }
  }
}
