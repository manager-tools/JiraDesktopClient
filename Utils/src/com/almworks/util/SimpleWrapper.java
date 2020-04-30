package com.almworks.util;

import org.jetbrains.annotations.Nullable;

// todo[IS:081120] there's already class called SimpleValueModel
//  yes, but it can be used only in AWTThread, and this wrapper is actually used in non-AWT thread, though in a single-thread manner.
/**
 * A simple wrapper around an arbitrary value to pass it as a mutable method parameter (useful to emulate multiple return values).
 * For simplicity's sake, non thread-safe it must be used only in the thread of creation.
 * @author igor baltiyskiy
 */
public class SimpleWrapper<T> implements ValueWrapper<T> {
  @Nullable
  private T myValue;
  private final Thread myThread;

  public SimpleWrapper() {
    this(null);
  }

  public SimpleWrapper(T value) {
    myValue = value;
    myThread = Thread.currentThread();
  }

  public static <T> ValueWrapper<T> create() {
    return new SimpleWrapper<T>();
  }
  
  public static <T> ValueWrapper<T> create(@Nullable T value) {
    return new SimpleWrapper<T>(value);
  }

  @Nullable
  public T getValue() {
    assertCreationThread();
    return myValue;
  }

  public void setValue(@Nullable T value) {
    assertCreationThread();
    myValue = value;
  }

  @Override
   public String toString() {
    return String.valueOf(myValue);
  }

  // todo[sank] semantics of equals() and hashCode() -? do we account for thread equality?

  /**
   * Asserts that the object is accessed from the thread of creation.
   */
  private void assertCreationThread() {
    assert Thread.currentThread().equals(myThread) : "used from thread " + Thread.currentThread() + " instead of " + myThread;
  }
}
