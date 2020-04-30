package com.almworks.util.model;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ScalarModelEvent <T> extends ContentModelEvent {
  private final T myOldValue;
  private final T myNewValue;

  public ScalarModelEvent(ContentModel source, T oldValue, T newValue) {
    super(source);
    myOldValue = oldValue;
    myNewValue = newValue;
  }

  public T getOldValue() {
    return myOldValue;
  }

  public T getNewValue() {
    return myNewValue;
  }

  public static <T> ScalarModelEvent<T> create(ScalarModel<T> scalarModel, T oldValue, T value) {
    return new ScalarModelEvent<T>(scalarModel, oldValue, value);
  }
}
