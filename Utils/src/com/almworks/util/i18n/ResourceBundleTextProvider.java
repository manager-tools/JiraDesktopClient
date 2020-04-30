package com.almworks.util.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleTextProvider extends BundleTextProvider {
  private final String myBundleName;
  private final ClassLoader myClassLoader;

  public ResourceBundleTextProvider(String bundleName, Weight weight, ClassLoader classLoader) {
    super(weight);
    myBundleName = bundleName;
    myClassLoader = classLoader;
  }

  protected ResourceBundle loadBundle(Locale locale) {
    ResourceBundle bundle;
    try {
      bundle = ResourceBundle.getBundle(myBundleName, locale, myClassLoader);
    } catch (MissingResourceException e) {
      bundle = null;
    }
    return bundle;
  }

  public String toString() {
    return "RBTP(" + myBundleName + ")";
  }
}
