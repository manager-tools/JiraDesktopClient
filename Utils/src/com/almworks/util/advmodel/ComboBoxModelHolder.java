package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public class ComboBoxModelHolder<T> extends DelegatingNoSubscriptionModel<T> implements AComboboxModel<T> {
  private final FireEventSupport<SelectionListener> mySelectionListeners =
    FireEventSupport.create(SelectionListener.class);

  @Nullable
  private AComboboxModel<T> myComboModel;
  private final SegmentedListModel<T> myModel = SegmentedListModel.create(AListModel.EMPTY);
//  private final DetachComposite myModelLife = new DetachComposite();
  private final Lifecycle myLife = new Lifecycle(false);

  public ComboBoxModelHolder() {
//    myModel.addListener(mySelectionListeners.getDispatcher());
  }

  public Detach setModel(final AComboboxModel<T> model) {
    myLife.cycleEnd();
    T oldSelection = getSelectedItem();
    myModel.setSegment(0, model == null ? AListModel.EMPTY : model);
    myComboModel = model;
    if (myModel.getSize() > 0)
      mySelectionListeners.getDispatcher().onInsert(0, myModel.getSize());
    if (model != null) {
      myLife.cycleStart();
      model.addSelectionListener(myLife.lifespan(), mySelectionListeners.getDispatcher());
    }
    if (!Util.equals(oldSelection, model != null ? model.getSelectedItem() : null)) {
      mySelectionListeners.getDispatcher().onSelectionChanged();
    }
    return myLife.getCurrentCycleDetach();
  }


  protected AListModel<T> getDelegate() {
    return myModel;
  }

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return myModel.addRemovedElementListener(listener);
  }

  public Detach addListener(Listener<? super T> l) {
    DetachComposite life = new DetachComposite();
    SelectionListener sl = l instanceof SelectionListener ? (SelectionListener) l : new ListListenerWrapper(l);
    mySelectionListeners.addStraightListener(life, sl);
    return life;
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    addSelectionListener(life, new ChangeListenerAdapter(gate, listener));
  }

  public void removeFirstListener(final Condition<Listener> condition) {
    mySelectionListeners.removeFirstListener(new Condition<SelectionListener>() {
      public boolean isAccepted(SelectionListener value) {
        if (condition.isAccepted(value)) {
          return true;
        }
        if (!(value instanceof ListListenerWrapper)) {
          return false;
        }
        return condition.isAccepted(((ListListenerWrapper) value).getListener());
      }
    });
  }

  @NotNull
  protected Listener getListenerDispatcher() {
    return mySelectionListeners.getDispatcher();
  }

  public T getSelectedItem() {
    return myComboModel != null ? myComboModel.getSelectedItem() : null;
  }

  public void setSelectedItem(T item) {
    if (myComboModel != null)
      myComboModel.setSelectedItem(item);
  }

  @Nullable
  public AComboboxModel<T> getComboModel() {
    return myComboModel;
  }

  public void addSelectionListener(Lifespan lifespan, SelectionListener listener) {
    mySelectionListeners.addStraightListener(lifespan, listener);
  }

  public void addSelectionChangeListener(Lifespan life, ChangeListener listener) {
    if (life.isEnded())
      return;
    addSelectionListener(life, new SelectionChangedAdapter(listener));
  }

  public static <T> ComboBoxModelHolder<T> create() {
    return new ComboBoxModelHolder<T>();
  }

  private static class ChangeListenerAdapter extends AROList.ChangeListenerAdapter implements SelectionListener{
    public ChangeListenerAdapter(ThreadGate gate, ChangeListener listener) {
      super(gate, listener);
    }

    public void onSelectionChanged() {
      fireChanged();
    }
  }
}
