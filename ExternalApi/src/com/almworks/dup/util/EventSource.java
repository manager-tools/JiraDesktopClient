package com.almworks.dup.util;

import org.almworks.util.detach.Lifespan;

/**
 * EventSource is a point where you can subscribe to any event. Of course, any given source will fire
 * only certain types of events.
 * <p>
 * <code>EE</code> type parameter is used only to designate which events should be expected from the
 * EventSource. This type parameter is not used in methods.
 * <p>
 * EventSource uses lifespans to detach listeners. That's why there's no removeListener().
 * <p>
 * NB: in some cases, listener may be called right when subscription is done, even before addListener() returns!
 *
 * @see Lifespan
 */
public interface EventSource<EE extends Event> {
  /**
   * Adds common listener that will receive all events.
   *
   * @param life [not null] life span of listener attachment
   * @param listener [not null] listener
   */
  void addListener(Lifespan life, EventListener<Event> listener);

  /**
   * Adds a listener that is able to receive only specific events.
   *
   * @param life [not null] life span of listener attachment
   * @param eventClass [not null] class of events that the listener can process
   * @param listener [not null] listener
   */
  <E extends Event> void addListener(Lifespan life, Class<E> eventClass, EventListener<E> listener);
}
