package com.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Break extends CheapCheckedException {
  public Break(String message) {
    super(message);
  }

  public Break() {}

  public static <T> T breakIf(boolean condition, T goodResult, String msg, Object... args) throws Break {
    if(condition) {
      throw new Break(String.format(msg, args));
    }
    return goodResult;
  }

  @Nullable("returns null if condition is false")
  public static <T> T breakIf(boolean condition, String msg, Object... args) throws Break {
    return breakIf(condition, (T) null, msg, args);
  }

  @NotNull("always throws, never returns")
  public static <T> T breakHere(String msg, Object... args) throws Break {
    return breakIf(true, (T) null, msg, args);
  }

  @NotNull("throws if null, returns t otherwise")
  public static <T> T breakIfNull(T t, String msg, Object... args) throws Break {
    return breakIf(t == null, t, msg, args);
  }
}
