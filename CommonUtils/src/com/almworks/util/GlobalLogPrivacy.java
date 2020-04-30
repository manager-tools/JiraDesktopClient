package com.almworks.util;

import org.jetbrains.annotations.NotNull;

public class GlobalLogPrivacy implements LogPrivacyPolizei {
  private static volatile LogPrivacyPolizei myLogPolizei = null;
  private static final GlobalLogPrivacy INSTANCE = new GlobalLogPrivacy();

  private GlobalLogPrivacy() {
  }

  public static String examineLogString(String string) {
    if (string == null)
      return string;
    LogPrivacyPolizei logPolizei = myLogPolizei;
    if (logPolizei != null) {
      string = logPolizei.examine(string);
    }
    return string;
  }

  public static synchronized void installPolizei(@NotNull LogPrivacyPolizei polizei) {
    myLogPolizei = new LogPrivacyPolizeiWrapper(polizei, myLogPolizei);
  }

  public static synchronized LogPrivacyPolizei getPolizei() {
    return INSTANCE;
  }

  @NotNull
  public synchronized String examine(@NotNull String message) {
    return examineLogString(message);
  }

  private static final class LogPrivacyPolizeiWrapper implements LogPrivacyPolizei {
    private final LogPrivacyPolizei myPolizei;
    private final LogPrivacyPolizei myNext;

    public LogPrivacyPolizeiWrapper(LogPrivacyPolizei polizei, LogPrivacyPolizei next) {
      myPolizei = polizei;
      myNext = next;
    }

    @NotNull
    public String examine(@NotNull String message) {
      message = myPolizei.examine(message);
      LogPrivacyPolizei next = myNext;
      if (next != null)
        message = next.examine(message);
      return message;
    }
  }
}
