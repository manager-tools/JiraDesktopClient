package com.almworks.util.properties;

import com.almworks.util.collections.Comparing;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public class TypedKeyWithEquality<T> extends TypedKey<T> {
  private final Object myEqualityClass;

  public TypedKeyWithEquality(String name, @NotNull Object equalityClass) {
    super(name, null, null);
    myEqualityClass = equalityClass;
  }

  public static <T> TypedKeyWithEquality<T> create(@NotNull Object equalityClass) {
    return new TypedKeyWithEquality<T>(String.valueOf(equalityClass), equalityClass);
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TypedKeyWithEquality<?>))
      return false;
    return Util.equals(myEqualityClass, ((TypedKeyWithEquality<?>) obj).myEqualityClass);
  }

  public int hashCode() {
    return Comparing.hashCode(myEqualityClass);
  }

  public Object getEqualityClass() {
    return myEqualityClass;
  }
}
