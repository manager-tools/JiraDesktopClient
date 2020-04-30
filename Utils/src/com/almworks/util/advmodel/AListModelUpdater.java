package com.almworks.util.advmodel;

import com.almworks.util.DECL;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dyoma
 */
public class AListModelUpdater <T> {
  private final Bottleneck myModelUpdater;
  private final OrderListModel<T> myModel = new OrderListModel<T>();
  private final Object myLock = new Object();
  private final Set<T> myToUpdate = Collections15.hashSet();
  private final Set<T> myToRemove = Collections15.hashSet();
  private final Set<T> myToAdd = Collections15.hashSet();
  private final List<Runnable> myWaitingForPendingUpdates = Collections15.arrayList();

  private int myAddingElements = 0;
  private int myNextWait;

  // confined to AWT
  private boolean myUpdatingReentrancyGuard;

  public AListModelUpdater(int period) {
    this(period, 0);
  }

  /**
   * @param period minimum time between updates
   * @param initialWait minimum time between first event and first update
   */
  public AListModelUpdater(int period, int initialWait) {
    myNextWait = initialWait;
    myModelUpdater = new Bottleneck(period, ThreadGate.AWT, new Runnable() {
      public void run() {
        updateModel();
      }
    });
  }

  public AListModel<T> getModel() {
    return myModel;
  }

  public void updateElement(T element) {
    synchronized (myLock) {
      if (myToRemove.contains(element))
        return;
      myToUpdate.add(element);
    }
    requestUpdate();
  }

  @ThreadAWT
  public void updateElementsAndFlush(Collection<? extends T> elements) {
    Threads.assertAWTThread();
    synchronized (myLock) {
      if (!myToRemove.isEmpty()) {
        List<T> updated = Collections15.arrayList(elements);
        updated.removeAll(myToRemove);
        elements = updated;
      }
      if (elements.isEmpty()) return;
      myToUpdate.addAll(elements);
    }
    flush();
  }

  private void requestUpdate() {
    int wait = myNextWait;
    if (wait > 0) {
      myNextWait = 0;
      myModelUpdater.delay(wait);
    }
    myModelUpdater.request();
  }

  public void remove(T element) {
    DECL.assumeThreadMayBeAWT();
    Runnable[] waiting;
    synchronized (myLock) {
      myToAdd.remove(element);
      myToUpdate.remove(element);
      myToRemove.add(element);
      waiting = checkWaitingForPendingUpdates();
    }
    requestUpdate();
    runWaitingForPendingUpdates(waiting);
  }

  private void runWaitingForPendingUpdates(Runnable[] waiting) {
    if (waiting == null)
      return;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < waiting.length; i++) {
      Runnable runnable = waiting[i];
      try {
        runnable.run();
      } catch (RuntimeException e) {
        if (waiting.length == 1)
          throw e;
        else
          Log.error(e);
      }
    }
  }

  @Nullable
  private Runnable[] checkWaitingForPendingUpdates() {
    assert Thread.holdsLock(myLock);
    if (hasPendingUpdates())
      return null;
    if (myWaitingForPendingUpdates.size() == 0)
      return null;
    Runnable[] result = myWaitingForPendingUpdates.toArray(new Runnable[myWaitingForPendingUpdates.size()]);
    myWaitingForPendingUpdates.clear();
    return result;
  }

  private boolean hasPendingUpdates() {
    assert Thread.holdsLock(myLock);
    return myToAdd.size() > 0 || myToUpdate.size() > 0 || myToRemove.size() > 0 || myAddingElements > 0;
  }

  public void add(T element) {
    synchronized (myLock) {
      if (myToRemove.remove(element)) {
        myToUpdate.add(element);
        return;
      }
      myToAdd.add(element);
    }
    requestUpdate();
  }

  public void addAll(Collection<T> elements) {
    synchronized (myLock) {
      myToRemove.removeAll(elements);
      myToAdd.addAll(elements);
    }
    requestUpdate();
  }

  public void addAll(T[] elements) {
    addAll(Arrays.asList(elements));
  }

  public void updateAll() {
    synchronized (myLock) {
      myToUpdate.addAll(getAllElements());
    }
    requestUpdate();
  }

  @ThreadAWT
  public Collection<T> getAllElements() {
    Threads.assertAWTThread();
    Set<T> result = Collections15.hashSet();
    synchronized (myLock) {
      result.addAll(myModel.toList());
      result.addAll(myToAdd);
      result.removeAll(myToRemove);
    }
    return result;
  }

  /**
   * @return all elements that aren't requested for update yet.
   */
  @ThreadAWT
  public Collection<T> getAllNotChanged() {
    Threads.assertAWTThread();
    HashSet<T> result = Collections15.hashSet();
    synchronized(myLock) {
      result.addAll(myModel.toList());
      result.removeAll(myToRemove);
      result.removeAll(myToUpdate);
    }
    return result;
  }

  protected void onElementsRemoved(List<T> elements) {
  }

  public void removeAll() {
    Threads.assertAWTThread();
    T[] toAddCopy;
    List<T> remove;
    Runnable[] waiting;
    synchronized (myLock) {
      toAddCopy = toArray(myToAdd);
      myToUpdate.clear();
      remove = Collections15.arrayList(myModel.toList());
      myToAdd.clear();
      myToRemove.addAll(remove);
      waiting = checkWaitingForPendingUpdates();
    }
    if (toAddCopy != null) {
      onElementsRemoved(Arrays.asList(toAddCopy));
    }
    requestUpdate();
    runWaitingForPendingUpdates(waiting);
  }

  @ThreadAWT
  private void updateModel() {
    Threads.assertAWTThread();
    if (myUpdatingReentrancyGuard) {
      myModelUpdater.delay();
      myModelUpdater.request();
      return;
    }
    myUpdatingReentrancyGuard = true;
    try {
      Set<T> update;
      T[] remove;
      T[] add;
      Runnable[] waiting;
      synchronized (myLock) {
        update = toSet(myToUpdate);
        remove = toArray(myToRemove);
        add = toArray(myToAdd);
        waiting = checkWaitingForPendingUpdates();
      }

      if (remove != null) {
        myModel.removeAll(remove);
        onElementsRemoved(Arrays.asList(remove));
      }

      if (update != null) {
        for (T elem : update) {
          int index = myModel.indexOf(elem);
          if (index >= 0)
            myModel.replaceAt_NoFire(index, elem);
        }
        fireUpdate(update);
      }

      if (add != null) {
        myModel.addAll(add);
      }
      runWaitingForPendingUpdates(waiting);
    } finally {
      myUpdatingReentrancyGuard = false;
    }
  }

  private void fireUpdate(Set<T> update) {
    int minUpdate = Integer.MAX_VALUE;
    int maxUpdate = Integer.MIN_VALUE;
    int size = myModel.getSize();
    for (int i = 0; i < size; i++) {
      if (update.contains(myModel.getAt(i))) {
        minUpdate = i;
        maxUpdate = i;
        break;
      }
    }
    if (minUpdate < Integer.MAX_VALUE && update.size() > 1) {
      for (int i = size - 1; i >= 0; i--) {
        if (update.contains(myModel.getAt(i))) {
          maxUpdate = i;
          break;
        }
      }
    }
    if (minUpdate <= maxUpdate) {
      myModel.updateRange(minUpdate, maxUpdate);
    }
  }

  @Nullable
  private T[] toArray(Collection<T> collection) {
    assert Thread.holdsLock(myLock);
    if (collection.size() == 0)
      return null;
    T[] update = (T[]) new Object[collection.size()];
    collection.toArray(update);
    collection.clear();
    return update;
  }

  @Nullable
  private Set<T> toSet(Collection<T> collection) {
    assert Thread.holdsLock(myLock);
    if (collection.size() == 0)
      return null;
    Set<T> result = Collections15.hashSet(collection);
    collection.clear();
    return result;
  }

  public void runWhenNoPendingUpdates(final ThreadGate gate, final Runnable runnable) {
    boolean runNow;
    synchronized (myLock) {
      runNow = !hasPendingUpdates();
      if (!runNow) {
        if (gate == ThreadGate.AWT || gate == ThreadGate.AWT_IMMEDIATE) {
          myWaitingForPendingUpdates.add(runnable);
        } else {
          myWaitingForPendingUpdates.add(new Runnable() {
            public void run() {
              gate.execute(runnable);
            }
          });
        }
      }
    }
    if (runNow)
      gate.execute(runnable);
  }

  /**
   * Lets updater know that an element will be added soon (if set is true), or
   * that previously forewarned adding of an element has been completed (if set to false).
   */
  public void addingElement(boolean set) {
    Runnable[] runnables;
    synchronized(myLock) {
      myAddingElements += set ? 1 : -1;
      runnables = checkWaitingForPendingUpdates();
    }
    runWaitingForPendingUpdates(runnables);
  }

  @ThreadAWT
  public void flush() {
    myModelUpdater.abort();
    updateModel();
  }

  public static <T> AListModelUpdater<T> create(int period) {
    return new AListModelUpdater<T>(period);
  }
}
