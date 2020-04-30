package com.almworks.util.i18n.text;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public abstract class I18NAccessor {
  public static final I18NAccessor EMPTY = new I18NAccessor() {
    @NotNull
    @Override
    public String getString(String suffix, Locale locale) {
      return "";
    }

    @Override
    public String toString() {
      return "<EMPTY>";
    }
  };

  @NotNull
  public abstract String getString(String suffix, Locale locale);

  public final I18NAccessor sub(String prefix) {
    return new Sub(this, prefix);
  }


  public static class Sub extends I18NAccessor {
    private final I18NAccessor myParent;
    private final String myPrefix;

    public Sub(I18NAccessor parent, String prefix) {
      myParent = parent;
      myPrefix = prefix;
    }

    @Override
    public String toString() {
      return myParent + "." + myPrefix;
    }

    @NotNull
    @Override
    public String getString(String suffix, Locale locale) {
      return myParent.getString(myPrefix + "." + suffix, locale);
    }
  }
}
