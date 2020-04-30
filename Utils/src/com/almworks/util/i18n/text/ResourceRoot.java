package com.almworks.util.i18n.text;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ResourceRoot extends I18NAccessor {
  private final ResourceInfo myInfo;

  public ResourceRoot(ResourceInfo info) {
    myInfo = info;
  }

  @NotNull
  @Override
  public String getString(String suffix, Locale locale) {
    return myInfo.getString(suffix, locale);
  }

  @Override
  public String toString() {
    return myInfo.toString();
  }

  public static ResourceRoot create(ClassLoader loader, String resource) {
    return new ResourceRoot(new ResourceInfo(loader, resource));
  }
}
