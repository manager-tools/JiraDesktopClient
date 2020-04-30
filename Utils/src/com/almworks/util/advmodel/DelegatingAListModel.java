package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Detach;

/**
 * @author dyoma
 */
public abstract class DelegatingAListModel<T> extends DelegatingNoSubscriptionModel<T> {

  public Detach addRemovedElementListener(RemovedElementsListener<T> listener) {
    return getDelegate().addRemovedElementListener(listener);
  }

  public Detach addListener(Listener<? super T> listener) {
    return getDelegate().addListener(listener);
  }

  public void removeFirstListener(Condition<Listener> condition) {
    getDelegate().removeFirstListener(condition);
  }
}
