package com.almworks.util;

import com.almworks.util.events.DefaultFiringStrategy;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class LazySubscription<L> {
  private final Subscriber mySubscriber;
  private final DefaultFiringStrategy<L> myStrategy;
  private final L myDispatcher;
  private final Object myLock = new Object();
  private Boolean mySubscribed = false;

  public LazySubscription(Subscriber subscriber, Class<L> listenerClass) {
    mySubscriber = subscriber;
    myStrategy = new DefaultFiringStrategy<L>();
    //noinspection unchecked
    myDispatcher = (L) Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[] {listenerClass},
      new BaseInvocationHandler() {
        protected Object invokeTarget(Method method, Object[] args) {
          return myStrategy.fireFromDispatcher(getListeners(), method, args);
        }
      });
  }
  
  public static <L> LazySubscription<L> create(Class<L> listenerClass, Subscriber subscriber) {
    return new LazySubscription<L>(subscriber, listenerClass);
  }

  public void addListener(Lifespan life, ThreadGate gate, final L listener) {
    if (life.isEnded() || listener == null) return;
    synchronized (myLock) {
      waitStableState();
      myStrategy.addListener(Lifespan.FOREVER, gate, listener);
    }
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        removeListener(listener);
      }
    });
    updateSubscription();
  }

  public void removeListener(L listener) {
    synchronized (myLock) {
      waitStableState();
      myStrategy.removeListener(listener);
    }
    updateSubscription();
  }

  public L getDispatcher() {
    return myDispatcher;
  }

  private void updateSubscription() {
    boolean required;
    boolean state;
    synchronized (myLock) {
      state = waitStableState();
      required = myStrategy.getListenersCount() > 0;
      if (state == required) return;
      mySubscribed = null;
    }
    try {
      if (required) mySubscriber.subscribe();
      else mySubscriber.unsubscribe();
      state = required;
    } finally {
      synchronized (myLock) {
        mySubscribed = state;
      }
    }
  }

  private boolean waitStableState() {
    while (mySubscribed == null)
      try {
        myLock.wait(30);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    return mySubscribed;
  }

  private Pair[] getListeners() {
    synchronized (myLock) {
      return myStrategy.getListeners();
    }
  }

  public interface Subscriber {
    void subscribe();

    void unsubscribe();
  }
}
