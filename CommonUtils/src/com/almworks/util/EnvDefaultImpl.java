package com.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public class EnvDefaultImpl implements EnvImpl {
  @Nullable
  @Override
  public String getProperty(@NotNull String key) {
    return System.getProperty(key, null);
  }

  @NotNull
  @Override
  public Collection<String> getPropertyKeys() {
    return System.getProperties().stringPropertyNames();
  }

  @Override
  public void changeProperties(@NotNull Map<String, String> diff) {
    System.getProperties().putAll(diff);
  }
}
