package com.almworks.items.impl;

public class ImplUtil {
  private static final ThreadLocal<Boolean> DBTHREAD = new ThreadLocal<Boolean>();

  public static void setDbThread() {
    DBTHREAD.set(Boolean.TRUE);
  }

  public static boolean isDbThread() {
    Boolean v = DBTHREAD.get();
    return v != null && v;
  }
}
