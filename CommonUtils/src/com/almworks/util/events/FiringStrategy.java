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
public interface FiringStrategy <L> {
  Object fireFromDispatcher(Pair/*<L, ThreadGate>*/[] listeners, Method method, Object[] args);

  Object fireFromDipatcherSnapshot(Pair/*<L, ThreadGate>*/[] listeners, Method method, Object[] args,
    ProcessingLock processingLock);

  L returningDispatcher(L dispatcher);

  L returningDispatcherSnapshot(L dispatcher);

  boolean addListener(Lifespan life, ThreadGate callbackGate, L listener);

  void removeListener(L listener);

  Pair/*<L, ThreadGate>*/[] getListeners();

  void noMoreEvents();

  boolean isNoMoreEvents();

  int getListenersCount();
}
