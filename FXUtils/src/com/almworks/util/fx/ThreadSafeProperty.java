package com.almworks.util.fx;

import com.almworks.util.LogHelper;
import com.almworks.util.threads.ThreadFX;
import com.almworks.util.threads.ThreadSafe;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;

/**
 * Contains an observable values and allows thread safe access to the current value.
 * Note: a new value becomes available via {@link #getValue()} before any listener is notified
 * @author dyoma
 */
public class ThreadSafeProperty<T> {
  private final SimpleObjectProperty<T> myProperty;
  private volatile T myValue;

  public ThreadSafeProperty() {
    myProperty = new SimpleObjectProperty<T>();
  }

  @ThreadSafe
  public T getValue() {
    return myProperty.get();
  }

  /**
   * Provides the observable value. The returned object is not thread safe and MUST be used from FX thread only.
   * @return observable
   */
  public ObservableObjectValue<T> getObservable() {
    return myProperty;
  }

  /**
   * Sets new value and notifies listeners
   * @param value new value
   */
  @ThreadFX
  public void setValue(T value) {
    if (!Platform.isFxApplicationThread()) LogHelper.error("Not FX thread", Thread.currentThread());
    myValue = value;
    myProperty.set(value);
  }
}
