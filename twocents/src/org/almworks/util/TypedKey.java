package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * TypedKey is a key for a typed parameter. Most often it is used for storing in maps values of different types.
 * <p/>
 * NB: equals() and hashCode() are intentionally not implemented, typed keys have object identity.
 *
 * @author dyoma
 * @author sereda
 */
public class TypedKey<T> implements Serializable {
  public static final TypedKey<?>[] EMPTY_ARRAY = new TypedKey[0];

  @NotNull
  private final String myName;

  @Nullable
  private final Class<T> myValueClass;

  @Nullable
  private final transient TypedKeyRegistry myRegistry;

  /**
   * Creates typed key. Don't use protected overloaded constructors, use single form.
   *
   * @param name debug name of the key
   * @param valueClass class of values; if null, runtime class checks are not possible
   * @param registry allows to register key in a map, allowing to restore key by name
   */
  protected TypedKey(@NotNull String name, @Nullable Class<T> valueClass, @Nullable TypedKeyRegistry registry) {
    myName = name;
    myValueClass = valueClass;
    myRegistry = registry;
    if (registry != null) {
      registry.add(this);
    }
  }

  @NotNull
  public static <T> TypedKey<T> create(String name) {
    return new TypedKey<T>(name, null, null);
  }

  @NotNull
  public static <T> TypedKey<T> create(Class<T> clazz) {
    return new TypedKey<T>(clazz.getName(), clazz, null);
  }

  @NotNull
  public static <T> TypedKey<T> create(String name, TypedKeyRegistry<TypedKey<T>> registry) {
    return new TypedKey<T>(name, null, registry);
  }

  @NotNull
  public static <T> TypedKey<T> create(String name, Class<T> valueClass) {
    return new TypedKey<T>(name, valueClass, null);
  }

  @Nullable
  public static <T extends TypedKey<?>> T findKey(List<? extends T> keys, String name) {
    if (keys == null || keys.isEmpty() || name == null) return null;
    for (T key : keys) {
      if (key == null) continue;
      if (name.equals(key.getName())) return key;
    }
    return null;
  }

  @NotNull
  public final String getName() {
    return myName;
  }

  @Nullable
  public Class<T> getValueClass() {
    return myValueClass;
  }

  @NotNull
  public String toString() {
    return myName;
  }

  public boolean isClassAware() {
    return myValueClass != null;
  }

  public boolean isRegistered() {
    return myRegistry != null;
  }

  @Nullable
  public T getFrom(Map<? extends TypedKey, ?> map) {
    Object value = map.get(this);
    return cast(value);
  }

  @Nullable
  public T putTo(Map<? extends TypedKey, ?> map, @Nullable T value) {
    //noinspection RawUseOfParameterizedType
    Object oldValue = ((Map) map).put(this, value);
    return cast(oldValue);
  }

  @Nullable
  public T removeFrom(Map<? extends TypedKey, ?> map) {
    Object oldValue = map.remove(this);
    return cast(oldValue);
  }

  @Nullable
  public final T cast(@Nullable Object object) {
    assert checkInstance(object);
    return (T) object;
  }

  private boolean checkInstance(Object object) {
    if (object != null && myValueClass != null && !myValueClass.isInstance(object)) {
      assert false : object + "[" + object.getClass() + "] is not an instance of " + myValueClass + " (" + this + ")";
    }
    return true;
  }

  public void copyFromTo(Map<TypedKey<?>, ?> from, Map<TypedKey<?>, ?> to) {
    T value = getFrom(from);
    if (value == null) removeFrom(to);
    else putTo(to, value);
  }
}


