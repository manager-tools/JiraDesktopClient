package com.almworks.util.model;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;

/**
 * :todoc:
 *
 * @author sereda
 */
public class BasicScalarModel <T>
  extends AbstractContentModel<ScalarModelEvent<T>, ScalarModel.Consumer<T>, ScalarModel<T>, ScalarModelSetter<T>>
  implements ScalarModel<T>, ScalarModelSetter<T> {

  public static final ScalarModel<Boolean> ALWAYS_TRUE = BasicScalarModel.createWithValue(Boolean.TRUE, false);
  public static final ScalarModel<Boolean> ALWAYS_FALSE = BasicScalarModel.createWithValue(Boolean.FALSE, false);
  public static final ScalarModel ALWAYS_NULL = BasicScalarModel.createWithValue(null, false);

  private final boolean myThrowIfNoValue;
  private final T myEmptySentinel;
  private final Synchronized<T> myValue;
  private final Synchronized<Thread> myReentrancyLock = new Synchronized<Thread>(null);

  /**
   * The number of shared locks taken by addListener(), or -1 if exclusive lock is taken by setValue
   * Protected by myReentrancyLock
   */
  private int myReentrancyLockSharedCounter = 0;

  protected BasicScalarModel(T value, boolean allowChange, boolean throwIfNoValue, T emptySentinel, boolean valueKnown) {
    super(valueKnown, allowChange, (Class) ScalarModel.Consumer.class);
    myValue = new Synchronized<T>(value, myLock);
    myThrowIfNoValue = throwIfNoValue;
    myEmptySentinel = emptySentinel;
  }

  public static <T> BasicScalarModel<T> create(boolean allowChange, boolean throwIfNoValue) {
    return new BasicScalarModel<T>(null, allowChange, throwIfNoValue, null, false);
  }

  public static <T> BasicScalarModel<T> create(boolean allowChange) {
    return create(allowChange, false);
  }

  public static <T> BasicScalarModel<T> create() {
    return create(true, false);
  }

  public static <T> BasicScalarModel<T> createWithValue(T value, boolean allowChange) {
    return new BasicScalarModel<T>(value, allowChange, false, null, true);
  }

  public static <T> BasicScalarModel<T> createConstant(@Nullable T value) {
    return createWithValue(value, false);
  }

  public static <T> BasicScalarModel<T> createModifiable(T value) {
    return createWithValue(value, true);
  }

  public static <T> BasicScalarModel<T> create(boolean allowChange, T emptySentinel) {
    return new BasicScalarModel<T>(emptySentinel, allowChange, false, emptySentinel, false);
  }

  public T getValue() throws NoValueException {
    synchronized (myLock) {
      if (!isContentKnown()) {
        if (myThrowIfNoValue)
          throw new NoValueException();
        return myEmptySentinel;
      }
      return getValueInternal();
    }
  }

  protected final T getValueInternal() {
    return myValue.get();
  }

  public void setValue(T value) throws ValueAlreadySetException {
    doSetValue(value, null, false);
  }

  public boolean commitValue(T previousValue, T value) throws ValueAlreadySetException {
    return doSetValue(value, previousValue, true);
  }

  protected boolean doSetValue(T value, T presumedOldValue, boolean checkOldValue) {
    lockReentrancy(true);
    try {
      Consumer<T> dispatcher;
      ScalarModelEvent<T> event;
      boolean contentLearned;
      synchronized (myLock) {
        if (isContentKnown() && !isContentChangeable())
          throw new ValueAlreadySetException();
        //    if (value == myEmptySentinel)
        //      throw new IllegalArgumentException("you cannot set value to empty sentinel");
        T oldValue = myValue.get();
        if (checkOldValue) {
          if (!Util.equals(oldValue, presumedOldValue))
            return false;
        }
        myValue.set(value);
        contentLearned = myContentKnown.commit(false, true);
        event = ScalarModelEvent.create(this, oldValue, getValueInternal());
        dispatcher = myEventSupport.getDispatcherSnapshot();
        if (!isContentChangeable())
          myEventSupport.noMoreEvents();
      }
      dispatcher.onScalarChanged(event);
      if (contentLearned)
        dispatcher.onContentKnown(event);
    } finally {
      unlockReentrancy(true);
    }
    return true;
  }

  private void lockReentrancy(boolean exclusive) {
    Thread thread = Thread.currentThread();
    synchronized (myReentrancyLock) {
      while (true) {
        Thread settingThread = myReentrancyLock.get();
        if (settingThread == null) {
          myReentrancyLock.set(thread);
          myReentrancyLockSharedCounter = exclusive ? -1 : 1;
          break;
        } else if (settingThread.equals(thread)) {
          if (exclusive || myReentrancyLockSharedCounter < 0)
            throw new IllegalStateException("same thread reentrancy is not allowed for setValue()/addListener()");
          myReentrancyLockSharedCounter++;
          break;
        } else {
          try {
            myReentrancyLock.wait();
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          }
        }
      }
    }
  }

  private void unlockReentrancy(boolean exclusive) {
    synchronized (myReentrancyLock) {
      Thread settingThread = myReentrancyLock.get();
      assert settingThread == Thread.currentThread();
      myReentrancyLockSharedCounter = exclusive ? 0 : myReentrancyLockSharedCounter - 1;
      assert myReentrancyLockSharedCounter >= 0;

      if (myReentrancyLockSharedCounter == 0) {
        myReentrancyLock.set(null);
        myReentrancyLock.notifyAll();
      }
    }
  }

  public T getValueBlocking() throws InterruptedException {
    myContentKnown.waitForValue(true);
    return getValue();
  }

  public T waitValue(Condition<T> condition) throws InterruptedException {
    synchronized (myLock) {
      myContentKnown.waitForValue(true);
      myValue.waitForCondition(condition);
      return getValue();
    }
  }

  public Object afterAddListenerWithLock(ThreadGate threadGate, Consumer<T> consumer, Object passThrough) {

    return isContentKnown() ? getValueInternal() : myEmptySentinel;
  }

  public void afterAddListenerWithoutLock(ThreadGate threadGate, final Consumer<T> consumer, Object passThrough) {
    T value = (T) passThrough;
    if (value != myEmptySentinel) {
      final ScalarModelEvent<T> event = ScalarModelEvent.create(BasicScalarModel.this, myEmptySentinel,
        getValueInternal());
      lockReentrancy(false);
      try {
        threadGate.execute(new Runnable() {
          public void run() {
            consumer.onScalarChanged(event);
            consumer.onContentKnown(event);
          }
        });
      } finally {
        unlockReentrancy(false);
      }
    }
  }

  public ScalarModelEvent<T> createDefaultEvent() {
    return new ScalarModelEvent<T>(this, myEmptySentinel, getValueInternal());
  }

  /**
   * Adapts model events to listener events. Listener receives events in the specified gate.
   * @param model source
   * @param listener listener that is invoked when source model generates events
   */
  public static void invokeOnChange(ScalarModel<?> model, Lifespan life, ThreadGate gate, final ChangeListener listener) {
    model.getEventSource().addListener(life, gate, new ScalarModel.Adapter() {
      @Override
      public void onScalarChanged(ScalarModelEvent event) {
        listener.onChange();
      }
    });
  }
}
