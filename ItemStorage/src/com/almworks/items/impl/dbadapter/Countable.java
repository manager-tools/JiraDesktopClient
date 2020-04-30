package com.almworks.items.impl.dbadapter;

import com.almworks.util.threads.ThreadSafe;

/**
 * Countable is something that is capable of providing a count of some property (eg., amount of items in the container) to the GUI.
 * To track count changes, one may add a listener with some priority which is the measure of importance of this particular count value to the GUI.
 * To receive notifications about changes of a group of countables, see CounterManager.
 * @author igor baltiyskiy
 */
public interface Countable extends PrioritizedListenerSupport<Countable.CountableListener> {
  /**
   * Getter for the Count property. If the count is unknown by the time this method is called, the method returns -1.
   * @return current amount of items conforming to the condition or -1 if it is unknown.
   */
  @ThreadSafe
  int getCount();

  /**
   * Listener for the changed events.
   */
  interface CountableListener {
    void onCountChanged();
  }

  /**
   * A listener that does nothing with the changed event.
   * Objects of this class may be useful when only the priority of the countable is important.
   */
  CountableListener DUMB = new CountableListener() {
    public void onCountChanged() {
    }
  };
}