package com.almworks.items.impl.dbadapter;

import org.almworks.util.TypedKey;
import org.almworks.util.TypedKeyRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DBProperty<T> extends TypedKey<T> {
  protected DBProperty(@NotNull String guid, @Nullable Class<T> valueClass) {
    super(guid, valueClass, null);
  }

  protected DBProperty(@NotNull String guid, @Nullable Class<T> valueClass, TypedKeyRegistry registry) {
    super(guid, valueClass, registry);
  }

  public static <T> DBProperty<T> create(@NotNull String guid, Class<T> valueClass) {
    assert isValidClass(valueClass) : valueClass;
    return new DBProperty<T>(guid, valueClass);
  }

  private static boolean isValidClass(Class valueClass) {
    return valueClass.equals(String.class) || valueClass.equals(Integer.class) || valueClass.equals(Long.class);
  }
}
