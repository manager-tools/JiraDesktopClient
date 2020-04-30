package com.almworks.util.i18n;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public abstract class LocalTextProvider {
  @NotNull
  public abstract Weight getWeight();

  @Nullable
  public abstract String getText(@NotNull String key, @NotNull Locale locale);

  public static enum Weight {
    DEFAULT,
    USER,
    SYSTEM
  }
}
