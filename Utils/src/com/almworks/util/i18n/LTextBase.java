package com.almworks.util.i18n;

import org.almworks.util.Log;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class LTextBase {
  protected final String myKey;
  protected final String myDefaultFormat;
  private static final boolean LTEXT_DEBUG = false;

  public LTextBase(String key, String defaultFormat) {
    assert key != null;
    assert defaultFormat != null;
    myKey = key;
    myDefaultFormat = defaultFormat;
  }

  public String getKey() {
    return myKey;
  }

  public abstract ResourceBundle getBundle(String key);

  protected String loadString() {
    String result = myDefaultFormat;
    ResourceBundle bundle = getBundle(myKey);
    if (bundle != null) {
      try {
        result = bundle.getString(myKey);
      } catch (MissingResourceException e) {
        if (LTEXT_DEBUG)
          Log.debug("cannot find " + myKey + " in " + bundle);
      }
    }
    return result;
  }

  protected String formatString(Object[] params) {
    String pattern = loadString();
    try {
      MessageFormat format = new MessageFormat(pattern, LTextUtils.SINGLE_LOCALE);
      return format.format(params);
    } catch (IllegalArgumentException e) {
      assert false : pattern + "\n" + e;
      Log.warn(pattern, e);
      return pattern + " (" + Arrays.toString(params) + ")";
    }
  }
}
