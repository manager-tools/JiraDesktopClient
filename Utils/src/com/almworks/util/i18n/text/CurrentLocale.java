package com.almworks.util.i18n.text;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.events.FireEventSupport;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class CurrentLocale extends LocalizedAccessor {
  @NotNull
  private static volatile Locale ourLocale = Locale.getDefault();
  private static FireEventSupport<ChangeListener> ourListeners = FireEventSupport.create(ChangeListener.class);
  private final I18NAccessor myAccessor;

  public CurrentLocale(I18NAccessor accessor) {
    myAccessor = accessor;
  }

  @NotNull
  @Override
  public String getString(String suffix) {
    return myAccessor.getString(suffix, ourLocale);
  }

  @Override
  public String toString() {
    return myAccessor.toString();
  }

  public static LocalizedAccessor createAccessor(ClassLoader loader, String resource) {
    return new CurrentLocale(ResourceRoot.create(loader, resource));
  }

  public static void addAWTListener(Lifespan life, ChangeListener listener) {
    ourListeners.addAWTListener(life, listener);
  }

  public static void setLocale(Locale locale) {
    if (locale == null) locale = Locale.getDefault();
    if (Util.equals(ourLocale, locale)) return;
    CurrentLocale.ourLocale = locale;
    ourListeners.getDispatcher().onChange();
  }

  @Override
  public Locale getCurrentLocale() {
    return getLocale();
  }

  @NotNull
  public static Locale getLocale() {
    return ourLocale;
  }
}
