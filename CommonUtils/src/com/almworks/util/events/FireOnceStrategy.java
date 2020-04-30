package com.almworks.util.events;

import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;

import java.lang.reflect.Method;

/**
 * :todoc:
 *
 * @author sereda
 */
public class FireOnceStrategy<L> implements FiringStrategy<L> {
  private boolean mySnapshotDispatched = false;
  private final DefaultFiringStrategy<L> myDelegate;

  public FireOnceStrategy(DefaultFiringStrategy<L> delegate) {
    if (delegate == null)
      throw new NullPointerException("delegate");
    myDelegate = delegate;
  }

  public L returningDispatcherSnapshot(L dispatcher) {
    L result = myDelegate.returningDispatcherSnapshot(dispatcher);
    myDelegate.noMoreEvents();
    return result;
  }

  public Object fireFromDispatcher(Pair[] listeners, Method method, Object[] args) {
    if (!myDelegate.checkFire())
      return null;
    myDelegate.noMoreEvents();
    myDelegate.doDispatch(listeners, method, args, ProcessingLock.DUMMY);
    return null;
  }

  public Object fireFromDipatcherSnapshot(Pair[] listeners, Method method, Object[] args,
    ProcessingLock processingLock)
  {
    assert myDelegate.isNoMoreEvents();
    if (!mySnapshotDispatched) {
      mySnapshotDispatched = true;
      myDelegate.doDispatch(listeners, method, args, processingLock);
    }
    return null;
  }

  public L returningDispatcher(L dispatcher) {
    return myDelegate.returningDispatcher(dispatcher);
  }

  public boolean addListener(Lifespan life, ThreadGate callbackGate, L listener) {
    return myDelegate.addListener(life, callbackGate, listener);
  }

  public void removeListener(L listener) {
    myDelegate.removeListener(listener);
  }

  public Pair/*<L, ThreadGate>*/[] getListeners() {
    return myDelegate.getListeners();
  }

  public void noMoreEvents() {
    myDelegate.noMoreEvents();
  }

  public boolean isNoMoreEvents() {
    return myDelegate.isNoMoreEvents();
  }

  public int getListenersCount() {
    return myDelegate.getListenersCount();
  }
}
