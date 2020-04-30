package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author dyoma
 */
public class Setting<T> extends TypedKey<T> {
  private final T myDefaulValue;

  protected Setting(String name, T defaultValue) {
    super(name, null, null);
    myDefaulValue = defaultValue;
  }

  public T getFrom(@NotNull PropertyMap propertyMap) {
    return propertyMap.containsKey(this) ? propertyMap.get(this) : myDefaulValue;
  }

  public T getFrom(Map<? extends TypedKey, ?> map) {
    return map.containsKey(this) ? super.getFrom(map) : myDefaulValue;
  }

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass"})
  public static <T> Setting<T> create(String name, T defaultValue) {
    return new Setting<T>(name, defaultValue);
  }
}
