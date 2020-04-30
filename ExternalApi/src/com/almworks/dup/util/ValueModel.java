package com.almworks.dup.util;

/**
 * ValueModel is a simple container of T. Subscribe to events() to
 * receive ValueEvents when this model gets updated.
 * <p>
 * The model is read-only. Write access is available in implementing classes.
 *
 * @see SimpleValueModel
 */
public interface ValueModel<T> {
  /**
   * Returns current value in the model. May return null.
   */
  T getValue();

  /**
   * Constant provider of model event. Subscribing to event will cause immediate notification about the
   * current value of the model.
   */
  EventSource<ValueEvent> events();
}
