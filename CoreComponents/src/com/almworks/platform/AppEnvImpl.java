package com.almworks.platform;

import com.almworks.api.install.Setup;
import com.almworks.util.EnvImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

class AppEnvImpl implements EnvImpl {
  @Nullable
  @Override
  public String getProperty(@NotNull String key) {
    return Setup.getStringProperty(key);
  }

  @NotNull
  @Override
  public Collection<String> getPropertyKeys() {
    return Setup.getPropertyKeys();
  }

  @Override
  public void changeProperties(@NotNull Map<String, String> diff) {
    Setup.changeProperties(diff);
  }
}
