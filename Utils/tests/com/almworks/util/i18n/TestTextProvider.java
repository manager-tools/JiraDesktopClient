package com.almworks.util.i18n;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * @author dyoma
 */
public class TestTextProvider extends LocalTextProvider {
  private final Map<String, String> myMap = Collections15.hashMap();

  @NotNull
  public Weight getWeight() {
    return Weight.DEFAULT;
  }

  @Nullable
  public String getText(@NotNull String key, @NotNull Locale locale) {
    return myMap.get(key);
  }

  public void put(String key, String value) {
    myMap.put(key, value);
  }
}
