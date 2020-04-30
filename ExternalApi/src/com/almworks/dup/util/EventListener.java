package com.almworks.dup.util;

/**
 * Listener interface that accepts events of type E. 
 */
public interface EventListener<E extends Event> {
  void onEvent(E event);
}
