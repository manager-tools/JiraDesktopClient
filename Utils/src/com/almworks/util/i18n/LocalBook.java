package com.almworks.util.i18n;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public abstract class LocalBook {
  @Nullable
  public abstract String get(@NotNull String key, @NotNull Locale locale);

  public abstract void installProvider(LocalTextProvider provider);

  public abstract void uninstallProvider(LocalTextProvider provider);
}
