package com.almworks.util.events;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

public class DetachableValue<T> {
  private T myValue;

  public static <T> DetachableValue<T> create() {
    return new DetachableValue<T>();
  }

  public void set(Lifespan lifespan, final T value) {
    if (lifespan.isEnded()) {
      myValue = null;
    } else {
      myValue = value;
      lifespan.add(new Detach() {
        protected void doDetach() {
          if (myValue == value) {
            myValue = null;
          }
        }
      });
    }
  }

  @Nullable
  public T get() {
    return myValue;
  }
}
