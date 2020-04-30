package com.almworks.dup.util;

/**
 * Detach is an interface that is implemented to perform end-of-life or end-of-attachment
 * actions. It is usually added to lifespan or detach composite.
 *
 * @see Detach
 * @see Lifespan
 * @see DetachComposite
 */
public interface Detachable {
  /**
   * Notifies detachable that detach starts. Event listeners are advised to ignore events after
   * this call. If detach is composite, some other detachable may cause events to come to a listener
   * before detach() is called. Listeners may also check isEnding() method on Detach class.
   *
   * Implementor MUST NOT perform any activity that could affect other classes. Only private flag
   * changes and notification of others about upcoming detach is available.
   */
  void preDetach();

  /**
   * Performs detach.
   */
  void detach();
}
