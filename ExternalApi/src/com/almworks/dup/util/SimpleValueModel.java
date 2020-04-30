package com.almworks.dup.util;

import org.almworks.util.detach.Lifespan;

import java.awt.*;

/**
 * Simple implementation of ValueModel, not thread safe.
 * <p>
 * NB: Use in AWT thread only!
 */
public class SimpleValueModel<T> implements ValueModel<T>, EventProcessorMaster {
  private final boolean myIgnoreSame;
  private final EventProcessor myEventProcessor = new EventProcessor(this);

  private T myValue;

  public SimpleValueModel(T value) {
    this(value, true);
  }

  /**
   * @param value initial value
   * @param ignoreSame if true, setValue() called with equal value will not cause events to be fired.
   */
  public SimpleValueModel(T value, boolean ignoreSame) {
    myValue = value;
    myIgnoreSame = ignoreSame;
  }

  public SimpleValueModel() {
    this(null);
  }

  public static <T> SimpleValueModel<T> create(T value) {
    return new SimpleValueModel<T>(value);
  }

  public T getValue() {
    return myValue;
  }

  /**
   * Sets a new value into the model. If ignoreSame is true, setting the same value will not cause event to be fired.
   *
   * @param value new value, may be null
   */
  public void setValue(T value) {
    assert EventQueue.isDispatchThread();
    if (myIgnoreSame) {
      if (myValue == value || (myValue != null && myValue.equals(value)))
        return;
    }
    myValue = value;
    myEventProcessor.fireEvent(new ValueEvent<T>(myValue));
  }

  public EventSource<ValueEvent> events() {
    return myEventProcessor;
  }

  public <E extends Event> void afterListenerAdded(EventProcessor processor, Class<E> eventClass,
    EventListener<E> listener, Lifespan life)
  {
    if (ValueEvent.class.isAssignableFrom(eventClass))
      ((EventListener) listener).onEvent(new ValueEvent<T>(myValue));
  }
}
