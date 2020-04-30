package com.almworks.util.model;

import com.almworks.util.commons.Condition;
import com.almworks.util.events.AddListenerHook;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;

/**
 * LightScalarModel is a light-weight analog to BasicScalarModel. It takes much less space and does not provide
 * the following features:<p>
 * <ul>
 * <li>Requesting values;</li>
 * <li>Reentrancy locking (be careful!);</li>
 * <li>Freezing values;</li>
 * <li>Sentinels.</li>
 * </ul>
 * It follows general contract of {@link ScalarModel}, including calling onScalarChanged() immediately after addListener.
 * It never calls onValueKnown().
 */
public class LightScalarModel<T>
  implements ScalarModel<T>, ScalarModelSetter<T>, AddListenerHook<ScalarModel.Consumer>
{
  private FireEventSupport<Consumer> myEventSource;
  private T myValue;

  public LightScalarModel(T value) {
    myValue = value;
  }

  public Object beforeAddListenerWithoutLock(ThreadGate threadGate, Consumer listener) {
    return null;
  }

  public Object beforeAddListenerWithLock(ThreadGate threadGate, Consumer listener, Object passThrough) {
    return null;
  }

  public Object afterAddListenerWithLock(ThreadGate threadGate, Consumer listener, Object passThrough)
  {
    return myValue;
  }

  public void afterAddListenerWithoutLock(ThreadGate threadGate, final Consumer listener, Object passThrough)
  {
    T value = (T) passThrough;
    if (value != null) {
      final ScalarModelEvent<T> event = ScalarModelEvent.create(this, null, value);
      threadGate.execute(new Runnable() {
        public void run() {
          listener.onScalarChanged(event);
        }
      });
    }
  }

  public boolean isContentKnown() {
    return true;
  }

  public void requestContent() {
    throw new UnsupportedOperationException();
  }

  public boolean isContentChangeable() {
    return true;
  }


  public synchronized EventSource<Consumer<T>> getEventSource() {
    if (myEventSource == null)
      myEventSource = FireEventSupport.create(Consumer.class, this, false, this);
    return (EventSource) myEventSource;
  }

  public ScalarModel<T> getModel() {
    return this;
  }

  public EventSource<RequestConsumer<ScalarModelSetter<T>>> getRequestEventSource() {
    throw new UnsupportedOperationException();
  }

  public void setContentKnown() {
  }


  public Object getLock() {
    return this;
  }

  public synchronized T getValue() {
    return myValue;
  }

  public synchronized T getValueBlocking() throws InterruptedException {
    return waitValue(Condition.<T>notNull());
  }

  public T waitValue(Condition<T> condition) throws InterruptedException {
    synchronized (this) {
      waitForCondition(condition, 0);
      return myValue;
    }
  }

  public void setValue(T value) {
    doSetValue(value, null, false);
  }

  public synchronized boolean commitValue(T previousValue, T value) throws ValueAlreadySetException {
    return doSetValue(value, previousValue, true);
  }

  protected boolean doSetValue(T newValue, T presumedOldValue, boolean checkOldValue) {
    Consumer<T> dispatcher = null;
    ScalarModelEvent<T> event = null;
    synchronized (this) {
      T oldValue = myValue;
      if (checkOldValue && !Util.equals(oldValue, presumedOldValue)) {
        return false;
      }
      myValue = newValue;
      notifyAll();
      if (myEventSource != null) {
        event = ScalarModelEvent.create(this, oldValue, newValue);
        dispatcher = myEventSource.getDispatcherSnapshot();
      }
    }
    if (dispatcher != null && event != null) {
      dispatcher.onScalarChanged(event);
    }
    return true;
  }

  protected boolean waitForCondition(Condition<T> condition, long timeout) throws InterruptedException {
    long deadline = timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
    synchronized (this) {
      try {
        while (!condition.isAccepted(myValue)) {
          long time = deadline - System.currentTimeMillis();
          if (time <= 0)
            return false;
          wait(time);
        }
        return true;
      } finally {
        notify();
      }
    }
  }

  public static <T> LightScalarModel<T> create() {
    return new LightScalarModel<T>(null);
  }

  public static <T> LightScalarModel<T> create(T value) {
    return new LightScalarModel<T>(value);
  }

  public void noMoreEvents() {
    myEventSource.noMoreEvents();
  }
}
