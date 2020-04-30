package com.almworks.jira.provider3.custom.loadxml;

import com.almworks.util.LogHelper;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class ConfigKey<T> {
  private final TypedKey<T> myKey;

  public ConfigKey(TypedKey<T> key) {
    myKey = key;
  }

  public TypedKey<T> getKey() {
    return myKey;
  }

  public static ConfigKey<String> string(TypedKey<String> key) {
    return new ConfigKey<String>(key) {
      @Override
      public String parseValue(String strValue) {
        return strValue;
      }

      @Override
      public String getDisplayName() {
        return getKey().getName() + ": string";
      }
    };
  }

  public static ConfigKey<Boolean> bool(TypedKey<Boolean> key) {
    return new ConfigKey<Boolean>(key) {
      @Override
      public Boolean parseValue(String strValue) {
        if ("true".equals(strValue)) return true;
        else if ("false".equals(strValue)) return false;
        return null;
      }

      @Override
      public String getDisplayName() {
        return getKey() + ": bool";
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static ConfigKey create(TypedKey<?> typedKey) {
    if (typedKey == null) return null;
    Class<?> valueClass = typedKey.getValueClass();
    if (valueClass == null) {
      LogHelper.error("Missing value class", typedKey);
      return null;
    }
    if (valueClass == String.class) return string((TypedKey<String>) typedKey);
    if (valueClass == Boolean.class) return bool((TypedKey<Boolean>) typedKey);
    LogHelper.error("Unknown value class", valueClass, typedKey);
    return null;
  }

  public abstract T parseValue(String strValue);

  public boolean putToMap(Map<? extends TypedKey<?>, ?> map, T value) {
    T prev = myKey.putTo(map, value);
    return prev == null;
  }

  @Nullable
  public static ConfigKey<?> findKey(List<ConfigKey<?>> keys, String keyName) {
    if (keys == null || keyName == null) return null;
    for (ConfigKey<?> key : keys) {
      if (keyName.equals(key.getKey().getName())) return key;
    }
    return null;
  }

  public abstract String getDisplayName();
}
