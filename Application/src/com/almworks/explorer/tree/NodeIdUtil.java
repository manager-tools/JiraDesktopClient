package com.almworks.explorer.tree;

import com.almworks.util.config.Configuration;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

class NodeIdUtil {
  private static final String PREFIX = "prefix";
  private static final String COUNTER = "counter";

  private static Configuration ourConfig;
  private static int idCounter;
  private static String idPrefix;

  @NotNull
  public static synchronized String newUID() {
    Configuration config = ourConfig;
    if (config == null) {
      assert false;
      return String.valueOf(System.currentTimeMillis());
    }
    if (idPrefix == null) {
      initState(config);
    }
    String result = idPrefix + Integer.toHexString(idCounter++);
    config.setSetting(COUNTER, idCounter);
    return result;
  }

  private static void initState(Configuration config) {
    String prefix = config.getSetting(PREFIX, null);
    int counter = config.getIntegerSetting(COUNTER, 0) + 0x10;
    if (prefix == null || counter == 0) {
      prefix = createPrefix();
      config.setSetting(PREFIX, prefix);
    }
    idPrefix = prefix;
    idCounter = counter;
  }

  private static String createPrefix() {
    long now = System.currentTimeMillis();
    long day = (now / Const.DAY) & 0xFFL;
    long millis = now & 0xFFL;
    long x = (day << 8) | millis;
    return Long.toHexString(x) + "-";
  }

  public static void setConfig(Configuration config) {
    ourConfig = config;
  }
}
