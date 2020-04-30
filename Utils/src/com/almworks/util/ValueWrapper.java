package com.almworks.util;

import org.jetbrains.annotations.Nullable;

// todo[IS:081120] there's already class called ValueModel
/**
 * Wraps a value that is an instance of an arbitrary Java class.
 * @author igor baltiyskiy
 */
public interface ValueWrapper<T> {
  @Nullable
  T getValue();

  void setValue(@Nullable T value);
}
