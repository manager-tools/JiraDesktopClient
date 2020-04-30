package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Detach;

/**
 * @author dyoma
 */
public abstract class ADelegatingListModel<T> extends AROList<T> {
  private final AListModel<T> myDelegate;

  protected ADelegatingListModel(AListModel<? extends T> delegate) {
    myDelegate = (AListModel<T>) delegate;
  }

  public Detach addListener(Listener<? super T> listener) {
    return myDelegate.addListener(listener);
  }

  public Detach addRemovedElementListener(RemovedElementsListener<T> removedElementsListener) {
    return myDelegate.addRemovedElementListener(removedElementsListener);
  }

  public int getSize() {
    return myDelegate.getSize();
  }

  public T getAt(int index) {
    return myDelegate.getAt(index);
  }

  public void removeFirstListener(Condition<Listener> condition) {
    myDelegate.removeFirstListener(condition);
  }

  public void forceUpdateAt(int index) {
    myDelegate.forceUpdateAt(index);
  }
}
