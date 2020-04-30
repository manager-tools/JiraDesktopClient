package com.almworks.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ApacheLoggerInstaller {
  public static void install() {
    System.setProperty("org.apache.commons.logging.Log", ApacheLogger.class.getName());
    Level level = Env.getBoolean(GlobalProperties.DEBUG_HTTPCLIENT) ? Level.FINE : Level.WARNING;
    Logger.getLogger("httpclient.wire.content").setLevel(level);
    Logger.getLogger("httpclient.wire.header").setLevel(level);
  }
}
