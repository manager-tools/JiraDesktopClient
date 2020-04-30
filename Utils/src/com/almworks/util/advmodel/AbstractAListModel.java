package com.almworks.util.advmodel;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Condition;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractAListModel<T> extends AROList<T> {
  private Object/*Listener<T> || List<Listener<T>>*/ myListeners;
  private Object/*RemovedElementsListener<T> || List<RemovedElementsListener<T>>*/ myRemovedElementsListeners;

  private boolean myFiringNow = false;

  private final Listener<T> myDispatcher = new MyDispatcher<T>();

  public Detach addListener(final Listener<? super T> listener) {
    synchronized (this) {
      Object listeners = myListeners;
      if (listeners == null) {
        myListeners = listener;
      } else if (listeners instanceof Listener) {
        List<Object> list = Collections15.arrayList(2);
        list.add(listeners);
        list.add(listener);
        myListeners = list;
      } else if (listeners instanceof List) {
        ((List) listeners).add(listener);
      } else {
        assert false : listeners;
      }
    }
    return new Detach() {
      protected void doDetach() throws Exception {
        synchronized(AbstractAListModel.this) {
          Object listeners = myListeners;
          if (listeners == listener) {
            myListeners = null;
          } else if (listeners instanceof List) {
            List list = ((List) listeners);
            list.remove(listener);
            if (list.size() == 0) {
              myListeners = null;
            }
          }
        }
      }
    };
  }

  public Detach addRemovedElementListener(final RemovedElementsListener<T> listener) {
    synchronized (this) {
      Object listeners = myRemovedElementsListeners;
      if (listeners == null) {
        myRemovedElementsListeners = listener;
      } else if (listeners instanceof RemovedElementsListener) {
        List<Object> list = Collections15.arrayList(2);
        list.add(listeners);
        list.add(listener);
        myRemovedElementsListeners = list;
      } else if (listeners instanceof List) {
        ((List) listeners).add(listener);
      } else {
        assert false : listeners;
      }
    }
    return new Detach() {
      protected void doDetach() throws Exception {
        synchronized(AbstractAListModel.this) {
          Object listeners = myRemovedElementsListeners;
          if (listeners == listener) {
            myRemovedElementsListeners = null;
          } else if (listeners instanceof List) {
            List list = ((List) listeners);
            list.remove(listener);
            if (list.size() == 0) {
              myRemovedElementsListeners = null;
            }
          }
        }
      }
    };
  }

  public void removeFirstListener(Condition<Listener> condition) {
    synchronized (this) {
      Object listeners = myListeners;
      if (listeners != null) {
        if (listeners instanceof Listener) {
          if (condition.isAccepted((Listener) listeners)) {
            myListeners = null;
          }
        } else if (listeners instanceof List) {
          List list = ((List) listeners);
          for (int i = list.size() - 1; i >= 0; i--) {
            Listener<T> listener = (Listener<T>) list.get(i);
            if (condition.isAccepted(listener)) {
              list.remove(i);
              return;
            }
          }
        } else {
          assert false : listeners;
        }
      }
    }
  }

  public void forceUpdateAt(int index) {
    if (index < 0 || index >= getSize()) return;
    updateRange(index, index);
  }

  private Listener<T> getDispatcher() {
    return myDispatcher;
  }

  protected void fireRearrange(@Nullable AListEvent event) {
    if (event == null)
      return;
    Threads.assertAWTThread();
    try {
      assert assertBeforeFire();
      myDispatcher.onListRearranged(event);
    } finally {
      assert LogHelper.assertError(assertAfterFire());
    }
  }

  protected void fireUpdate(UpdateEvent event) {
    Threads.assertAWTThread();
    try {
      assert assertBeforeFire();
      myDispatcher.onItemsUpdated(event);
    } finally {
      assert LogHelper.assertError(assertAfterFire());
    }
  }

  protected void fireUpdate(int index) {
    fireUpdate(updateRangeEvent(index, index));
  }

  public void updateAt(int index) {
    updateRange(index, index);
  }

  protected void fireInsert(int index, int length) {
    try {
      assert assertBeforeFire();
      if (length != 0) {
        assert length > 0 : length;
        Threads.assertAWTThread();
        int size = getSize();
        getDispatcher().onInsert(index, length);
        assert size == getSize() : "Before: " + size + " after: " + getSize();
      }
    } finally {
      assert LogHelper.assertError(assertAfterFire());
    }
  }

  protected void fireRemoved(@NotNull RemovedEvent<T> event) {
    Threads.assertAWTThread();
    try {
      assert assertBeforeFire();
      int size = getSize();
      getDispatcher().onRemove(event.getFirstIndex(), event.getLength(), event);
      assert size == getSize() : "Before: " + size + " after: " + getSize();
    } finally {
      assert LogHelper.assertError(assertAfterFire());
    }
  }

  protected RemovedEvent<T> fireBeforeElementsRemoved(int firstIndex, List<? extends T> removedElements) {
    return fireBeforeElementsRemoved(RemoveNotice.create(firstIndex, removedElements));
  }

  protected RemovedEvent<T> fireBeforeElementsRemoved(RemoveNotice<T> notice) {
    int size = getSize();
    Object listeners = getRemovedListeners();
    if (listeners != null) {
      if (listeners instanceof RemovedElementsListener) {
        ((RemovedElementsListener) listeners).onBeforeElementsRemoved(notice);
        assert size == getSize() : listeners + " before: " + size + " after: " + getSize();
      } else if (listeners instanceof List) {
        for (Object listener : dupRemovedListeners(listeners)) {
          ((RemovedElementsListener) listener).onBeforeElementsRemoved(notice);
          assert size == getSize() : listener + " before: " + size + " after: " + getSize();
        }
      }
    }
    return notice.createPostRemoveEvent();
  }

  private Object[] dupRemovedListeners(Object listeners) {
    synchronized (this) {
      return ((List) listeners).toArray();
    }
  }

  private Object getRemovedListeners() {
    synchronized (AbstractAListModel.this) {
      return myRemovedElementsListeners;
    }
  }

  public void updateRange(int index1, int index2) {
    fireUpdate(updateRangeEvent(index1, index2));
  }

  public void updateAll() {
    if (getSize() == 0)
      return;
    updateRange(0, getSize() - 1);
  }

  public void updateAll(Collection<? extends T> elements) {
    if (getSize() == 0)
      return;
    Set<? extends T> set = Collections15.asSet(elements);
    int firstIndex = -1;
    for (int i = 0; i < getSize(); i++) {
      T item = getAt(i);
      boolean toUpdate = set.contains(item);
      if ((toUpdate && firstIndex != -1) || (!toUpdate && firstIndex == -1))
        continue;
      if (toUpdate)
        firstIndex = i;
      else {
        updateRange(firstIndex, i - 1);
        firstIndex = -1;
      }
    }
  }

  protected class SwapEvent extends RearrangeEvent {
    public SwapEvent(int index0, int index1) {
      super(index0, index1);
    }

    private SwapEvent(int index0, int index1, int translateIndex) {
      super(index0, index1, translateIndex);
    }

    protected int privateGetNewIndex(int oldIndex) {
      if (oldIndex == getLowAffectedIndex())
        return getHighAffectedIndex();
      if (oldIndex == getHighAffectedIndex())
        return getLowAffectedIndex();
      return oldIndex;
    }

    public AListEvent translateIndex(int diff) {
      return new SwapEvent(getLowAffectedIndex(), getHighAffectedIndex(), diff);
    }
  }


  public static class MoveEvent extends RearrangeEvent {
    private final boolean myMovedForward;

    public MoveEvent(int oldIndex, int newIndex) {
      super(oldIndex, newIndex);
      myMovedForward = newIndex >= oldIndex;
    }

    public MoveEvent(int oldIndex, int newIndex, int translateIndex) {
      super(oldIndex, newIndex, translateIndex);
      myMovedForward = newIndex >= oldIndex;
    }

    protected int privateGetNewIndex(int oldIndex) {
      int low = getLowAffectedIndex() - getTranslated();
      int high = getHighAffectedIndex() - getTranslated();
      assert oldIndex >= low && oldIndex <= high;
      if (myMovedForward) {
        return oldIndex == low ? high : oldIndex - 1;
      } else {
        return oldIndex == high ? low : oldIndex + 1;
      }
    }

    public AListEvent translateIndex(int diff) {
      return myMovedForward ? new MoveEvent(getLowAffectedIndex(), getHighAffectedIndex(), diff) :
        new MoveEvent(getHighAffectedIndex(), getLowAffectedIndex(), diff);
    }
  }

  private boolean assertBeforeFire() {
    boolean now = myFiringNow;
    if (!now) {
      myFiringNow = true;
    } else {
      // and throw assertion
      myFiringNow = false;
    }
    return !now;
  }

  private boolean assertAfterFire() {
    boolean now = myFiringNow;
    myFiringNow = false;
    return now;
  }

  private class MyDispatcher<T> implements Listener<T> {
    public void onInsert(int index, int length) {
      Object listeners = getListeners();
      if (listeners != null) {
        if (listeners instanceof Listener) {
          notifyInsert(index, length, (Listener) listeners);
        } else if (listeners instanceof List) {
          for (Object listener : dupListeners(listeners)) {
            notifyInsert(index, length, (Listener) listener);
          }
        } else LogHelper.error("Wrong listeners", listeners);
      }
    }

    private void notifyInsert(int index, int length, Listener listener) {
      try {
        listener.onInsert(index, length);
      } catch (Throwable e) {
        LogHelper.error(e);
      }
    }

    private Object getListeners() {
      synchronized (this) {
        return myListeners;
      }
    }

    private Object[] dupListeners(Object listeners) {
      synchronized (AbstractAListModel.this) {
        return ((List) listeners).toArray();
      }
    }

    public void onRemove(int index, int length, RemovedEvent<T> event) {
      Object listeners = getListeners();
      if (listeners != null) {
        if (listeners instanceof Listener) {
          notifyRemove(index, length, event, (Listener) listeners);
        } else if (listeners instanceof List) {
          for (Object listener : dupListeners(listeners)) {
            notifyRemove(index, length, event, (Listener) listener);
          }
        } else {
          assert false : listeners;
        }
      }
    }

    private void notifyRemove(int index, int length, RemovedEvent<T> event, Listener l) {
      try {
        l.onRemove(index, length, event);
      } catch (Throwable e) {
        LogHelper.error(e);
      }
    }

    public void onListRearranged(AListEvent event) {
      Object listeners = getListeners();
      if (listeners != null) {
        if (listeners instanceof Listener) {
          notifyRearrange(event, (Listener) listeners);
        } else if (listeners instanceof List) {
          for (Object listener : dupListeners(listeners)) {
            notifyRearrange(event, (Listener) listener);
          }
        } else {
          assert false : listeners;
        }
      }
    }

    private void notifyRearrange(AListEvent event, Listener listener) {
      try {
        listener.onListRearranged(event);
      } catch (Throwable e) {
        LogHelper.error(e);
      }
    }

    public void onItemsUpdated(UpdateEvent event) {
      Object listeners = getListeners();
      if (listeners != null) {
        if (listeners instanceof Listener) {
          notifyUpdate(event, (Listener) listeners);
        } else if (listeners instanceof List) {
          for (Object listener : dupListeners(listeners)) {
            notifyUpdate(event, (Listener) listener);
          }
        } else {
          assert false : listeners;
        }
      }
    }

    private void notifyUpdate(UpdateEvent event, Listener listener) {
      try {
        listener.onItemsUpdated(event);
      } catch (Exception e) {
        LogHelper.error(e);
      }
    }
  }
}
