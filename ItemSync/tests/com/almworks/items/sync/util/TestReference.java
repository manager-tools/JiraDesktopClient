package com.almworks.items.sync.util;

import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.TimeGuard;
import junit.framework.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TestReference<T> {
  private final AtomicReference<T> myValue = new AtomicReference<T>(null);
  private final AtomicBoolean myPublished = new AtomicBoolean(false);

  public static <T> TestReference<T> create() {
    return new TestReference<T>();
  }

  public boolean compareAndSet(T expected, T value) {
    boolean set = myValue.compareAndSet(expected, value);
    if (set) myPublished.set(true);
    return set;
  }

  public boolean deferValue(T value) {
    if (myPublished.get()) return false;
    myValue.set(value);
    return !myPublished.get();
  }

  public boolean publishValue() {
    return myPublished.compareAndSet(false, true);
  }

  public T waitForPublished() throws InterruptedException {
    return TimeGuard.waitFor(new Procedure<TimeGuard<T>>() {
      @Override
      public void invoke(TimeGuard<T> arg) {
        if (myPublished.get()) arg.setResult(myValue.get());
      }
    });
  }

  public boolean isPublished() {
    return myPublished.get();
  }

  public T getDeferredValue() {
    Assert.assertFalse(myPublished.get());
    return myValue.get();
  }
}
