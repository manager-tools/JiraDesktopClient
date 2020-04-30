package com.almworks.util;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public class TestEnvImpl implements EnvImpl {
  private Map<String, String> myProps = Collections15.hashMap();

  @Nullable
  @Override
  public String getProperty(@NotNull String key) {
    return myProps.get(key);
  }

  @NotNull
  @Override
  public Collection<String> getPropertyKeys() {
    return myProps.keySet();
  }

  public void setProperty(@NotNull String key, String value) {
    myProps.put(key, value);
  }

  @Override
  public void changeProperties(@NotNull Map<String, String> diff) {
    myProps.putAll(diff);
  }
}
