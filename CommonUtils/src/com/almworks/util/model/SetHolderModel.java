package com.almworks.util.model;

import com.almworks.integers.LongArray;
import com.almworks.util.collections.Containers;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SetHolderModel<T> implements SetHolder<T> {
  private final List<T> myElements = Collections15.arrayList();
  private final List<Listener<T>> myListeners = Collections15.arrayList(4);
  private final List<ThreadGate> myGates = Collections15.arrayList(4);
  private final List<EventImpl2<T>> myEventsInProgress = Collections15.arrayList(4);
  private final List<Set<T>> myHistoryAdded = Collections15.arrayList(4);
  private final List<Set<T>> myHistoryRemoved = Collections15.arrayList(4);
  private final LongArray myHistoryVersions = new LongArray();
  private long myActualVersion = 0;

  @Override
  public void addInitListener(Lifespan life, ThreadGate gate, final Listener<T> listener) {
    if (listener == null || gate == null || life.isEnded()) return;
    EventImpl2<T> event;
    synchronized (myElements) {
      if (myListeners.contains(listener)) return;
      myListeners.add(listener);
      myGates.add(gate);
      if (myElements.isEmpty()) event = null;
      else {
        event = new EventImpl2<T>(this, gate, listener);
        event.initActual(0, Collections15.<T>arrayList(myElements), Collections15.<T>emptyCollection(), myActualVersion);
        myEventsInProgress.add(event);
      }
    }
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        removeListener(listener);
      }
    });
    if (event != null) event.dispatch();
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  @Override
  public void removeListener(Listener<?> listener) {
    if (listener == null) return;
    synchronized (myElements) {
      int index = myListeners.indexOf(listener);
      if (index < 0) return;
      myListeners.remove(index);
      myGates.remove(index);
    }
  }

  /** @see #changeSet */
  public boolean add(T ... element) {
    return changeSet(Arrays.asList(element), null);
  }

  /** @see #changeSet */
  public boolean add(@Nullable Collection<T> elements) {
    return changeSet(elements, null);
  }

  /** @see #changeSet */
  public boolean remove(T ... element) {
    return changeSet(null, Arrays.asList(element));
  }

  /** @see #changeSet */
  public boolean remove(@Nullable Collection<T> elements) {
    return changeSet(null, elements);
  }

  /**
   * @return true if the set was changed as a result of this call
   */
  public boolean changeSet(@Nullable Collection<T> add, @Nullable Collection<T> remove) {
    if (add == null) add = Collections15.emptyCollection();
    if (remove == null) remove = Collections15.emptyCollection();
    Set<T> added = Collections15.hashSet(add);
    Set<T> removed = Collections15.hashSet(remove);
    added.removeAll(removed);
    EventImpl2<T>[] events;
    synchronized (myElements) {
      added.removeAll(myElements);
      removed.retainAll(myElements);
      if (added.isEmpty() && removed.isEmpty()) return false;
      myActualVersion++;
      myElements.addAll(added);
      myElements.removeAll(removed);
      if (myListeners.isEmpty()) {
        optimizeHistory();
        return true;
      }
      //noinspection unchecked
      events = new EventImpl2[myListeners.size()];
      for (int i = 0; i < myListeners.size(); i++) {
        events[i] = new EventImpl2<T>(this, myGates.get(i), myListeners.get(i));
        events[i].initActual(myActualVersion - 1, added, removed, myActualVersion);
      }
      myHistoryAdded.add(Collections.unmodifiableSet(added));
      myHistoryRemoved.add(Collections.unmodifiableSet(removed));
      myHistoryVersions.add(myActualVersion);
      myEventsInProgress.addAll(Arrays.asList(events));
    }
    Throwable exception = null;
    for (EventImpl2<T> event : events) exception = event.dispatch();
    if (exception != null) throw ExceptionUtil.rethrow(exception);
    return true;
  }

  private void optimizeHistory() {
    assert Thread.holdsLock(myElements);
    assert myHistoryAdded.size() == myHistoryRemoved.size() && myHistoryAdded.size() == myHistoryVersions.size();
    if (!myEventsInProgress.isEmpty()) return;
    myHistoryAdded.clear();
    myHistoryRemoved.clear();
    myHistoryVersions.clear();
  }

  public void eventDispatched(EventImpl2<T> event, boolean checkRemove) {
    synchronized (myElements) {
      boolean done = myEventsInProgress.remove(event);
      assert !checkRemove || done;
      optimizeHistory();
    }
  }

  private long collectChanges(long fromVersion, Collection<T> targetAdd, Collection<T> targetRemoved) {
    assert targetAdd.isEmpty() && targetRemoved.isEmpty();
    synchronized (myElements) {
      if (fromVersion > myActualVersion) {
        assert false : fromVersion + " " + myActualVersion;
        fromVersion = myActualVersion;
      }
      if (fromVersion == myActualVersion ) return myActualVersion;
      if (fromVersion == 0) {
        targetAdd.addAll(myElements);
        return myActualVersion;
      }
      if (myHistoryVersions.isEmpty()) {
        assert false;
        return myActualVersion;
      }
      if (myHistoryVersions.get(0) > fromVersion + 1)
        assert false : myHistoryVersions.get(0) + " " + fromVersion + 1;
      HashSet<T> add = Collections15.hashSet();
      HashSet<T> remove = Collections15.hashSet();
      for (int i = 0; i < myHistoryVersions.size(); i++) {
        if (myHistoryVersions.get(i) <= fromVersion) continue;
        Set<T> stepAdd = Collections15.hashSet(myHistoryAdded.get(i));
        Set<T> stepRemove = Collections15.hashSet(myHistoryRemoved.get(i));
        assert !Containers.containsAny(stepAdd, stepRemove);
        assert !Containers.containsAny(stepRemove, stepAdd);
        assert !Containers.containsAny(add, stepAdd);
        assert !Containers.containsAny(remove, stepRemove);
        for (Iterator<T> it = stepAdd.iterator(); it.hasNext();) {
          T e = it.next();
          if (remove.remove(e)) it.remove();
        }
        add.addAll(stepAdd);
        for (Iterator<T> it = stepRemove.iterator(); it.hasNext();) {
          T e = it.next();
          if (add.remove(e)) it.remove();
        }
        remove.addAll(stepRemove);
      }
      assert !Containers.containsAny(add, remove);
      assert !Containers.containsAny(remove, add);
      targetAdd.addAll(add);
      targetRemoved.addAll(remove);
      return myActualVersion;
    }
  }

  @NotNull
  public List<T> copyCurrent() {
    synchronized (myElements) {
      return Collections15.arrayList(myElements);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  void testPrintHistory() {
    synchronized (myElements) {
      for (int i = 0; i < myHistoryVersions.size(); i++) {
        long version = myHistoryVersions.get(i);
        List<T> add = Collections15.arrayList(myHistoryAdded.get(i));
        List<T> remove = Collections15.arrayList(myHistoryRemoved.get(i));
        System.out.println(version + ": +(" + TextUtil.separateToString(add, ",")  + ") -(" + TextUtil.separateToString(remove, ",") + ")");
      }
    }
  }

  @Override
  public boolean isEmpty() {
    synchronized (myElements) {
      return myElements.isEmpty();
    }
  }

  private static class EventImpl2<T> implements Event<T>, Runnable {
    private final SetHolderModel<T> myModel;
    private final Listener<T> myListener;
    private final ThreadGate myGate;
    private Collection<T> myAdded;
    private Collection<T> myRemoved;
    private long myFromVersion = -1;
    private long myToVersion = -1;

    private EventImpl2(SetHolderModel<T> model, ThreadGate gate, Listener<T> listener) {
      myModel = model;
      myGate = gate;
      myListener = listener;
    }

    @Override
    public SetHolder<T> getSet() {
      return myModel;
    }

    @NotNull
    @Override
    public Collection<T> getAdded() {
      return myAdded;
    }

    @NotNull
    @Override
    public Collection<T> getRemoved() {
      return myRemoved;
    }

    @Override
    public long getNewVersion() {
      return myToVersion;
    }

    @Override
    public void run() {
      try {
        myListener.onSetChanged(this);
      } finally {
        myModel.eventDispatched(this, true);
      }
    }

    @Override
    public long actualize(long fromVersion) {
      if (fromVersion < 0) fromVersion = 0;
      if (fromVersion == myFromVersion) return myToVersion;
      if (fromVersion == 0) {
        myFromVersion = fromVersion;
        synchronized (myModel.myElements) {
          myRemoved = Collections15.emptyCollection();
          myAdded = Collections15.unmodifiableListCopy(myModel.myElements);
          myToVersion = myModel.myActualVersion;
        }
        return myToVersion;
      }
      myAdded = Collections15.arrayList();
      myRemoved = Collections15.arrayList();
      myToVersion =  myModel.collectChanges(fromVersion, myAdded, myRemoved);
      myAdded = Collections.unmodifiableCollection(myAdded);
      myRemoved = Collections.unmodifiableCollection(myRemoved);
      myFromVersion = fromVersion;
      return myToVersion;
    }

    @Override
    public boolean isEmpty() {
      return myAdded.isEmpty() && myRemoved.isEmpty();
    }

    public Throwable dispatch() {
      try {
        myGate.execute(this);
        return null;
      } catch (Throwable e) {
        Log.error(e);
        myModel.eventDispatched(this, false); // Probably event was successfully dispatched straight and already removed. 
        return e;
      }
    }

    public void initActual(long fromVersion, Collection<T> added, Collection<T> removed, long toVersion) {
      myAdded = added;
      myRemoved = removed;
      myFromVersion = fromVersion;
      myToVersion = toVersion;
    }
  }
}
