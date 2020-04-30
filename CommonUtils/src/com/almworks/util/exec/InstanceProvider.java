package com.almworks.util.exec;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InstanceProvider<V> extends ContextDataProvider {
  private final V myInstance;
  private final TypedKey<? super V> myKey;
  private final Class myClass;

  public InstanceProvider(@NotNull V instance, @Nullable TypedKey<? super V> key) {
    myInstance = instance;
    myKey = key;
    myClass = instance.getClass();
  }

  public <T> T getObject(Class<T> objectClass, int depth) {
    if (objectClass == myClass || objectClass.isAssignableFrom(myClass)) {
      return (T) myInstance;
    } else {
      return null;
    }
  }

  public <T> T getObject(TypedKey<T> key, int depth) {
    if (key == myKey && key != null) {
      return (T) myInstance;
    } else {
      return null;
    }
  }

  public static <V> InstanceProvider<V> instance(V object) {
    return new InstanceProvider<V>(object, null);
  }

  public static <V> InstanceProvider<V> instance(V object, TypedKey<V> key) {
    return new InstanceProvider<V>(object, key);
  }
}
