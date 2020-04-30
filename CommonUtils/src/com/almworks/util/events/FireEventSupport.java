package com.almworks.util.events;

import com.almworks.util.BaseInvocationHandler;
import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;

public class FireEventSupport <L> extends EventSource<L> {
  protected final Class<L> myListenerClass;
  protected final FiringStrategy<L> myStrategy;
  protected final L myDispatcher;
  protected List<FireEventSupport<L>> myChained;

  protected FireEventSupport(Class<L> listenerClass, FiringStrategy<L> strategy) {
    if (listenerClass == null)
      throw new NullPointerException("listenerClass");
    if (strategy == null)
      throw new NullPointerException("strategy");
    myListenerClass = listenerClass;
    myStrategy = strategy;
    myDispatcher = createDispatcher(listenerClass);
  }

  public static <L> FireEventSupport<L> create(Class<L> listenerClass, Object lock, boolean isFireOnce,
    AddListenerHook<L> sink) {

    DefaultFiringStrategy baseStrategy =
      lock == null ? new DefaultFiringStrategy(sink) : new SynchronizedFiringStrategy(sink, lock);
    FiringStrategy strategy = !isFireOnce ? (FiringStrategy) baseStrategy : new FireOnceStrategy(baseStrategy);
    return new FireEventSupport<L>(listenerClass, strategy);
  }

  public static <L> FireEventSupport<L> createUnsynchronized(Class<L> listenerClass) {
    return create(listenerClass, null, false, null);
  }

  public static <L> FireEventSupport<L> createFireOnce(Class<L> listenerClass) {
    return create(listenerClass, new Object(), true, null);
  }

  public static <L> FireEventSupport<L> create(Class<L> listenerClass) {
    return createSynchronized(listenerClass);
  }

  public static <L> FireEventSupport<L> createSynchronized(Class<L> listenerClass) {
    return create(listenerClass, new Object(), false, null);
  }

  public boolean addListener(Lifespan life, ThreadGate callbackGate, L listener) {
    return myStrategy.addListener(life, callbackGate, listener);
  }

  public void removeListener(L listener) {
    myStrategy.removeListener(listener);
  }

  public void removeFirstListener(Condition<L> condition) {
    Pair[] listeners = myStrategy.getListeners();
    for (Pair pair : listeners) {
      L listener = (L) pair.getFirst();
      if (condition.isAccepted(listener)) {
        removeListener(listener);
        break;
      }
    }
  }

  public synchronized void addChainedSource(EventSource<L> eventSource) {
    if (!(eventSource instanceof FireEventSupport))
      throw new IllegalArgumentException(eventSource.toString());
    if (myChained == null)
      myChained = Collections15.arrayList();
    myChained.add((FireEventSupport<L>) eventSource);
  }

  public synchronized void removeChainedSource(EventSource<L> eventSource) {
    if (myChained == null)
      return;
    myChained.remove(eventSource);
  }

  public L getDispatcher() {
    return myStrategy.returningDispatcher(myDispatcher);
  }

  /**
   * Creates temporary dispatcher that would notify only those listeners that existed at
   * the moment of calling this method. This dispatcher is independent from FireEventSupport
   * object that produced it, and could be used in thread-unsafe context.
   *
   * @return a temporary dispatcher object with a copy of listeners collection
   */
  public L getDispatcherSnapshot() {
    return getDispatcherSnapshot(ProcessingLock.DUMMY);
  }

  /**
   * Similar to getDispatcherSnapshot(), but it will also acquire and release passed ProcessingLock
   * while dispatching event and gating into listeners.
   * <p>
   * By contract, ProcessingLock will return to its original state when all processing is done and all listeners
   * has been called.
   */
  public L getDispatcherSnapshot(final ProcessingLock processingLock) {
    L result = (L) Proxy.newProxyInstance(myListenerClass.getClassLoader(), new Class[]{myListenerClass}, new BaseInvocationHandler() {
      private final Pair[] myCopiedListeners = getListeners();
      private final Object[] myCopiedChainedDispatchers = getChainedDispatchers(true, processingLock);

      protected Object invokeTarget(Method method, Object[] args) {
        Object result = myStrategy.fireFromDipatcherSnapshot(myCopiedListeners, method, args, processingLock);
        if (myCopiedChainedDispatchers != null)
          fireChained(myCopiedChainedDispatchers, method, args);
        return result;
      }
    });

    return myStrategy.returningDispatcherSnapshot(result);
  }

  public int getListenersCount() {
    return myStrategy.getListenersCount();
  }

  public boolean isNoMoreEvents() {
    return myStrategy.isNoMoreEvents();
  }

  public void noMoreEvents() {
    myStrategy.noMoreEvents();
  }

  private L createDispatcher(Class<L> listenerClass) {
    return (L) Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, new BaseInvocationHandler() {
      protected Object invokeTarget(Method method, Object[] args) {
        Object result = myStrategy.fireFromDispatcher(getListeners(), method, args);
        if (myChained != null && myChained.size() > 0)
          fireChained(getChainedDispatchers(false, ProcessingLock.DUMMY), method, args);
        return result;
      }
    });
  }

  private void fireChained(Object[] dispatchers, Method method, Object[] args) {
    if (dispatchers == null)
      return;
    for (int i = 0; i < dispatchers.length; i++) {
      Object dispatcher = dispatchers[i];
      try {
        method.invoke(dispatcher, args);
      } catch (Exception e) {
        Log.warn(e);
      }
    }
  }

  private Object[] getChainedDispatchers(boolean takeSnapshot, ProcessingLock processingLock) {
    if (myChained == null || myChained.size() == 0)
      return null;
    Object[] dispatchers = new Object[myChained.size()];
    int i = 0;
    for (Iterator<FireEventSupport<L>> ii = myChained.iterator(); ii.hasNext();) {
      FireEventSupport<L> eventSupport = ii.next();
      dispatchers[i++] = takeSnapshot ? eventSupport.getDispatcherSnapshot(processingLock) : eventSupport.getDispatcher();
    }
    return dispatchers;
  }

  private final Pair[] getListeners() {
    return myStrategy.getListeners();
  }
}
