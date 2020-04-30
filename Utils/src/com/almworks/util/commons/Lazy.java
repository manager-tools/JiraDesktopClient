package com.almworks.util.commons;

import org.jetbrains.annotations.NotNull;

public abstract class Lazy <T> implements Factory<T> {
  private final boolean myThreadSafe;
  private T myInstance;
  private boolean mySet = false;

  public Lazy(boolean threadSafe) {
    myThreadSafe = threadSafe;
  }

  public Lazy() {
    this(true);
  }

  @NotNull
  public final T get() {
    return myThreadSafe ? getSafe() : getUnsafe();
  }

  public final T create() {
    return get();
  }

  @NotNull
  public Object getLock() {
    return this;
  }

  public final boolean isInitialized() {
    if (myThreadSafe) {
      synchronized (this) {
        return mySet;
      }
    } else {
      return mySet;
    }
  }

  @NotNull
  protected abstract T instantiate();

  private T getSafe() {
    synchronized (this) {
      return getUnsafe();
    }
  }

  private T getUnsafe() {
    if (!mySet) {
      myInstance = instantiate();
      mySet = true;
    }
    return myInstance;
  }
}
