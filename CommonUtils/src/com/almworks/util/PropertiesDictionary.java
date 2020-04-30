package com.almworks.util;

import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.Set;

public abstract class PropertiesDictionary {
  private static Set<String> myProperties = Collections15.hashSet();

  protected static String register(String property) {
    myProperties.add(property);
    return property;
  }

  public static boolean hasProperty(String property) {
    return myProperties.contains(property);
  }

  public static Set<String> allProperties() {
    return Collections.unmodifiableSet(myProperties);
  }

  protected PropertiesDictionary() {}
}
