package com.almworks.util.model;


/**
 * :todoc:
 *
 * @author sereda
 */
public interface ScalarModelSetter <T> extends ModelSetter<ScalarModel<T>, ScalarModelSetter<T>> {
  /**
   * You cannot call setValue() from a notification.
   */
  void setValue(T value) throws ValueAlreadySetException;

  /**
   * Sets new value only if the previous value was exactly as passed in the parameter
   *
   * @return true if value was set
   */
  boolean commitValue(T previousValue, T value) throws ValueAlreadySetException;
}
