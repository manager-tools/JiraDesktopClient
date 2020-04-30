package com.almworks.api.container;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.Lifespan;

public interface EventRouter {
  Role<EventRouter> ROLE = Role.role("eventRouter");

  /**
   * Same as calling {@link #addListener(org.almworks.util.detach.Lifespan, com.almworks.util.exec.ThreadGate, Object, Class)} for each interface that listener implements.
   */
  void addListener(Lifespan life, ThreadGate gate, Object listener);

  <I, C extends I> void addListener(Lifespan life, ThreadGate gate, C listener, Class<I> listenerClass);

  void removeListener(Object listener);

  <I> I getEventSink(Class<I> listenerClass, boolean globalSink);
}
