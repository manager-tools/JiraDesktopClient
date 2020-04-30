package com.almworks.util.i18n;

import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class PropertiesBundleTextProvider extends BundleTextProvider {
  private final File myFile;
  private final Locale myLocale;
  private final String myKeyPrefix;

  public PropertiesBundleTextProvider(@NotNull File file, @NotNull Weight weight, @Nullable Locale locale,
    @Nullable String keyPrefix)
  {
    super(weight);
    myFile = file;
    myLocale = locale;
    myKeyPrefix = keyPrefix;
  }

  protected ResourceBundle loadBundle(Locale locale) {
    if (myLocale != null) {
      if (!myLocale.equals(locale))
        return null;
    }
    if (!myFile.isFile()) {
      Log.debug("cannot read bundle: no " + myFile);
      return null;
    }
    if (!myFile.canRead()) {
      Log.debug("cannot read bundle: " + myFile + " not readable");
      return null;
    }
    try {
      return new DecodingPropertyResourceBundle(myFile, myKeyPrefix);
    } catch (IOException e) {
      Log.debug("cannot read bundle: " + myFile + ": " + e);
      return null;
    }
  }

  public String toString() {
    return "PBTP(" + myFile + ")";
  }
}
