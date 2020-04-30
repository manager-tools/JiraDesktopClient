package com.almworks.dup.util;

import org.almworks.util.detach.Lifespan;

/**
 * This is a helper interface for coupling models with event processors.
 */
public interface EventProcessorMaster {
  <E extends Event> void afterListenerAdded(EventProcessor processor, Class<E> eventClass, EventListener<E> listener, Lifespan life);
}
