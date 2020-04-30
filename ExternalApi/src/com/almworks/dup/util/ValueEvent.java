package com.almworks.dup.util;

/**
 * Event bearing news about change in a ValueModel.
 *
 * @see ValueModel
 */
public class ValueEvent<T> extends Event {
  private final T myValue;

  public ValueEvent(T value) {
    myValue = value;
  }

  /**
   * Returns a new value that has been set.
   */
  public T getValue() {
    return myValue;
  }
}
