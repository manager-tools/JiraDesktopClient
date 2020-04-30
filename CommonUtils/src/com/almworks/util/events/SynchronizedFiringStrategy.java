package com.almworks.util.events;

import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SynchronizedFiringStrategy <L> extends DefaultFiringStrategy<L> {
  protected final Object myLock;

  public SynchronizedFiringStrategy(AddListenerHook<L> addListenerHook, Object lock) {
    super(addListenerHook);
    myLock = lock;
  }

  public SynchronizedFiringStrategy(Object lock) {
    super();
    myLock = lock;
  }

  public SynchronizedFiringStrategy(AddListenerHook<L> addListenerHook) {
    this(addListenerHook, new Object());
  }

  public SynchronizedFiringStrategy() {
    this(new Object());
  }

  public boolean addListener(Lifespan life, ThreadGate callbackGate, L listener) {
    if (listener == null)
      throw new NullPointerException("listener");
    if (callbackGate == null)
      throw new NullPointerException("callbackGate");

    boolean result = false;

    Object hookPassThrough = myAddListenerHook.beforeAddListenerWithoutLock(callbackGate, listener);
    synchronized (myLock) {
      hookPassThrough = myAddListenerHook.beforeAddListenerWithLock(callbackGate, listener, hookPassThrough);
      if (!isNoMoreEvents()) {
        doAddListener(life, callbackGate, listener);
        result = true;
      }
      hookPassThrough = myAddListenerHook.afterAddListenerWithLock(callbackGate, listener, hookPassThrough);
    }
    myAddListenerHook.afterAddListenerWithoutLock(callbackGate, listener, hookPassThrough);

    return result;
  }

  public void removeListener(L listener) {
    synchronized (myLock) {
      super.removeListener(listener);
    }
  }

  public Pair[] getListeners() {
    synchronized (myLock) {
      return super.getListeners();
    }
  }

  public void noMoreEvents() {
    synchronized (myLock) {
      super.noMoreEvents();
    }
  }

  public int getListenersCount() {
    synchronized (myLock) {
      return super.getListenersCount();
    }
  }
}
