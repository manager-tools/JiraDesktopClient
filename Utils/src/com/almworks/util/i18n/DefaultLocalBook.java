package com.almworks.util.i18n;

import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

public class DefaultLocalBook extends LocalBook {
  private static final Comparator<LocalTextProvider> PROVIDER_ORDER_BY_WEIGHT = new Comparator<LocalTextProvider>() {
    public int compare(LocalTextProvider o1, LocalTextProvider o2) {
      int w1 = o1.getWeight().ordinal();
      int w2 = o2.getWeight().ordinal();
      return w2 < w1 ? -1 : (w2 == w1 ? 0 : 1);
    }
  };

  private final Set<LocalTextProvider> myProviders = Collections15.treeSet(PROVIDER_ORDER_BY_WEIGHT);

  @Nullable
  public synchronized String get(@NotNull String key, @NotNull Locale locale) {
    for (LocalTextProvider provider : myProviders) {
      String r = provider.getText(key, locale);
      if (r != null)
        return r;
    }
    return null;
  }

  public synchronized void installProvider(LocalTextProvider provider) {
    myProviders.add(provider);
    Log.debug("installed " + provider);
  }

  public synchronized void uninstallProvider(LocalTextProvider provider) {
    myProviders.remove(provider);
    Log.debug("uninstalled " + provider);
  }
}
