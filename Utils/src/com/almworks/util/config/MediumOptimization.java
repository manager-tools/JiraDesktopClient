package com.almworks.util.config;

import javolution.util.SimplifiedFastMap;

public class MediumOptimization {
  private static final SimplifiedFastMap<String, String> ourInterned = SimplifiedFastMap.create();

  public static synchronized void addInternedNames(String ... names) {
    for (String name : names) {
      addInternedName(name);
    }
  }

  public static synchronized void addInternedName(String name) {
    String intern = name.intern();
    ourInterned.put(intern, intern);
  }

  public static String optimize(String name) {
    if (name == null)
      return name;
    String interned = ourInterned.get(name);
    return interned == null ? name : interned;
  }
}
