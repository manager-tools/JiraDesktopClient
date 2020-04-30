package com.almworks.util.i18n;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class BundleTextProvider extends LocalTextProvider {
  protected final Weight myWeight;
  private final Map<Locale, ResourceBundle> myBundles = Collections15.hashMap();

  public BundleTextProvider(Weight weight) {
    myWeight = weight;
  }

  @Nullable
  protected abstract ResourceBundle loadBundle(Locale locale);

  @Nullable
  public String getText(String key, Locale locale) {
    ResourceBundle bundle = getBundle(locale);
    try {
      return bundle == null ? null : bundle.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  @NotNull
  public Weight getWeight() {
    return myWeight;
  }

  @Nullable
  private synchronized ResourceBundle getBundle(Locale locale) {
    ResourceBundle bundle = myBundles.get(locale);
    if (bundle == null) {
      if (!myBundles.containsKey(locale)) {
        bundle = loadBundle(locale);
        myBundles.put(locale, bundle);
      }
    }
    return bundle;
  }
}
