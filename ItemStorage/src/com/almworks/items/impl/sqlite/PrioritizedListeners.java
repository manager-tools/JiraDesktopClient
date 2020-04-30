package com.almworks.items.impl.sqlite;

import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PrioritizedListeners<T> implements Iterable<T> {
  /**
   * Holds listeners in order of decreasing priority
   */
  private final CopyOnWriteArrayList<T> myListenersSorted = new CopyOnWriteArrayList<T>();

  /**
   * Holds the priority
   * todo mem-effective structure? not much lost here though
   */
  private final Map<T, Integer> myPriority = Collections15.hashMap();

  public static <T> PrioritizedListeners<T> create() {
    return new PrioritizedListeners<T>();
  }

  public synchronized Iterator<T> iterator() {
    return myListenersSorted.iterator();
  }

  public synchronized void add(T listener, int priority) {
    insert(listener, priority);
  }

  public synchronized void setPriority(int priority, T listener) {
    CopyOnWriteArrayList<T> ls = myListenersSorted;
    int idx = ls.indexOf(listener);
    if (idx < 0)
      return;
    myPriority.put(listener, priority);
    if ((idx > 0 && getPriority(idx - 1) < priority) || (idx < ls.size() - 1 && getPriority(idx + 1) > priority)) {
      // reinsert if out of order
      myListenersSorted.remove(idx);
      insert(listener, priority);
    }
  }

  @Nullable
  public synchronized Integer getPriorityOf(T listener) {
    return myPriority.get(listener);
  }

  public synchronized void setPriority(int priority, Condition<T> condition) {
    for (T listener : myListenersSorted) {
      if (condition.isAccepted(listener)) {
        setPriority(priority, listener);
      }
    }
  }

  public synchronized boolean remove(T listener) {
    myListenersSorted.remove(listener);
    myPriority.remove(listener);
    return !myListenersSorted.isEmpty();
  }

  public synchronized int getTotalPriority() {
    int passiveMax = Integer.MIN_VALUE;
    int activeTotal = 0;
    int len = myListenersSorted.size();
    for (int i = 0; i < len; i++) {
      int p = getPriority(i);
      if (p > 0)
        activeTotal += p;
      else
        passiveMax = Math.max(passiveMax, p);
    }
    int priority = activeTotal > 0 ? activeTotal : passiveMax;
    return priority;
  }

  private void insert(T listener, int priority) {
    assert Thread.holdsLock(this);
    int len = myListenersSorted.size();
    int i;
    for (i = 0; i < len; i++) {
      int p = getPriority(i);
      if (priority > p)
        break;
    }
    myListenersSorted.add(i, listener);
  }

  private int getPriority(int index) {
    assert Thread.holdsLock(this);
    Integer value = myPriority.get(myListenersSorted.get(index));
    int p = value == null ? 0 : value;
    return p;
  }

  public synchronized boolean isEmpty() {
    return myListenersSorted.isEmpty();
  }
}
