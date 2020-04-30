package com.almworks.util.events;

import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;

public class DefaultFiringStrategy<L> implements FiringStrategy<L> {
  private static final Pair[] EMPTY_ARRAY = {};
  private static final AddListenerHook EMPTY_HOOK = new AddListenerHook.Adapter();
  private ArrayList<Pair<L, ThreadGate>> myListeners = null;
  private volatile boolean myNoMoreEvents = false;
  private volatile Throwable myNoMoreEventsSetterStack = null;
  protected final AddListenerHook<L> myAddListenerHook;

  public DefaultFiringStrategy(AddListenerHook<L> addListenerHook) {
    myAddListenerHook = addListenerHook == null ? EMPTY_HOOK : addListenerHook;
  }

  public DefaultFiringStrategy() {
    this(null);
  }

  public Object fireFromDispatcher(Pair[] listeners, Method method, Object[] args) {
    if (!checkFire())
      return null;
    doDispatch(listeners, method, args, ProcessingLock.DUMMY);
    return null;
  }

  public Object fireFromDipatcherSnapshot(Pair[] listeners, Method method, Object[] args,
    ProcessingLock processingLock)
  {
    doDispatch(listeners, method, args, processingLock);
    return null;
  }

  public L returningDispatcher(L dispatch) {
    return dispatch;
  }

  public L returningDispatcherSnapshot(L dispatch) {
    if (!checkFire())
      return null;
    return dispatch;
  }

  public boolean addListener(Lifespan life, ThreadGate callbackGate, final L listener) {
    if (listener == null)
      throw new NullPointerException("listener");
    if (callbackGate == null)
      throw new NullPointerException("callbackGate");

    Object hookPassThrough = myAddListenerHook.beforeAddListenerWithoutLock(callbackGate, listener);
    hookPassThrough = myAddListenerHook.beforeAddListenerWithLock(callbackGate, listener, hookPassThrough);

    boolean result = false;
    if (!myNoMoreEvents) {
      doAddListener(life, callbackGate, listener);
      result = true;
    }
    hookPassThrough = myAddListenerHook.afterAddListenerWithLock(callbackGate, listener, hookPassThrough);
    myAddListenerHook.afterAddListenerWithoutLock(callbackGate, listener, hookPassThrough);
    return result;
  }

  protected final void doAddListener(Lifespan life, ThreadGate callbackGate, final L listener) {
    assert listener == null || listener.equals(listener) : listener;
    if (life.isEnded())
      return;
    if (myListeners == null)
      myListeners = new ArrayList<Pair<L, ThreadGate>>(1);
    myListeners.add(Pair.create(listener, callbackGate));
    if (life == Lifespan.FOREVER)
      return;
    life.add(new Detach() {
      protected void doDetach() {
        removeListener(listener);
      }
    });
  }

  /**
   * //TODO "equals()" or "==" ?
   */
  public void removeListener(L listener) {
    if (myListeners != null && listener != null)
      for (Iterator<Pair<L, ThreadGate>> iterator = myListeners.iterator(); iterator.hasNext();) {
        if (iterator.next().getFirst().equals(listener)) {
          iterator.remove();
          break;
        }
      }
  }

  public Pair/*<L, ThreadGate>*/[] getListeners() {
    if (myListeners == null || myListeners.size() == 0)
      return EMPTY_ARRAY;
    else
      return myListeners.toArray(new Pair[myListeners.size()]);
  }

  public void noMoreEvents() {
    if (myListeners != null) {
      myListeners.clear();
      myListeners = null;
    }
    if (!myNoMoreEvents) {
      myNoMoreEvents = true;
      myNoMoreEventsSetterStack = new Throwable();
    }
  }

  public final boolean isNoMoreEvents() {
    return myNoMoreEvents;
  }

  public int getListenersCount() {
    return myListeners == null ? 0 : myListeners.size();
  }

  protected boolean checkFire() {
    if (myNoMoreEvents) {
      assert throwNoMoreEventsViolation();
      return false;
    }
    return true;
  }

  private boolean throwNoMoreEventsViolation() {
    throw new Failure("noMoreEvents flag was set by: ", myNoMoreEventsSetterStack);
  }

  protected void doDispatch(Pair/*<L, ThreadGate>*/[] listeners, final Method method, final Object[] args,
    final ProcessingLock processingLock)
  {
    final int dispatchID = EventDebugger.isEnabled() ? EventDebugger.logStartDispatch(listeners, method, args) : -1;
    processingLock.lock(this);
    try {
      assert Modifier.isPublic(method.getDeclaringClass().getModifiers()) : method;
      for (Pair pair : listeners) {
        final Object listener = pair.getFirst();
        final ThreadGate gate = (ThreadGate) pair.getSecond();
        if (EventDebugger.isEnabled())
          EventDebugger.logDispatchBeforeGating(dispatchID, gate, listener, method, args);
        final LockOwner lockOwner = new LockOwner("DFS", listener);
        processingLock.lock(lockOwner);
        if (ThreadGate.isRightNow(gate))
          doInvokeListener(dispatchID, gate, listener, method, args, processingLock, lockOwner);
        else
          gate.execute(new Runnable() {
            public void run() {
              doInvokeListener(dispatchID, gate, listener, method, args, processingLock, lockOwner);
            }
          });
      }
    } finally {
      processingLock.release(this);
      if (EventDebugger.isEnabled())
        EventDebugger.logEndDispatch(dispatchID, listeners, method, args);
    }
  }

  private void doInvokeListener(int dispatchID, ThreadGate gate, Object listener, Method method, Object[] args,
    ProcessingLock processingLock, LockOwner lockOwner)
  {
    try {
      if (EventDebugger.isEnabled())
        EventDebugger.logDispatchInvocationStarts(dispatchID, gate, listener, method, args);
      method.invoke(listener, args);
    } catch (Throwable e) {
      if (EventDebugger.isEnabled())
        EventDebugger.logDispatchInvocationException(dispatchID, e, gate, listener, method, args);
      Throwable cause = ExceptionUtil.unwrapInvocationException(e);
      if (cause instanceof InterruptedException || cause instanceof RuntimeInterruptedException) {
        Log.debug(listener + " interrupted", e);
      } else {
        Log.error(listener, e);
      }
    } finally {
      if (EventDebugger.isEnabled())
        EventDebugger.logDispatchInvocationFinishes(dispatchID, gate, listener, method, args);
      processingLock.release(lockOwner);
    }
  }
}
