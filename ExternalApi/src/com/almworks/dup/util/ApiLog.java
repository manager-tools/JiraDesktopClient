package com.almworks.dup.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public final class ApiLog {
  private static final String LOGFILE_PATTERN = "eapi%g.log";
  private static final boolean ENABLED = !"true".equals(System.getProperty("eapi.debug.disable"));
  private static final int COUNT = 4;
  private static final int LIMIT = 10000000;

  private static Handler ourHandler = null;
  private static File ourLogDirectory = null;

  public static synchronized void configureLogging(File targetDirectory) {
    if (ourHandler != null) {
      try {
        ourHandler.close();
      } catch (Exception e) {
        // ignore
      }
      ourHandler = null;
    }
    ourLogDirectory = targetDirectory;
  }

  public static boolean isLogging() {
    return getHandler() != null;
  }

  public static boolean debug(String message, Throwable throwable) {
    return log(Level.INFO, message, throwable);
  }

  public static boolean debug(String message) {
    return debug(message, null);
  }

  public static boolean debug(Exception e) {
    return debug(null, e);
  }

  public static boolean error(Object object, Throwable throwable) {
    Handler handler = getHandler();
    if (handler == null)
      return true;
    String message;
    if (object == null)
      message = "";
    else if (object instanceof String)
      message = (String) object;
    else
      message = object.toString() + " (class " + object.getClass().getName() + ") caused error";
    handler.publish(createLogRecord(Level.SEVERE, message, throwable));
    return true;
  }

  public static void error(String message) {
    if (getHandler() != null)
     error(message, new Error(message));
  }

  public static boolean error(Throwable e) {
    return error(null, e);
  }

  public static void warn(String message, Throwable e) {
    log(Level.WARNING, message, e);
  }

  public static void warn(String message) {
    warn(message, null);
  }

  public static void warn(Throwable e) {
    warn("", e);
  }

  private static LogRecord createLogRecord(Level level, String message, Throwable throwable) {
    LogRecord logRecord = new LogRecord(level, message);
    logRecord.setThrown(throwable);
    logRecord.setSourceClassName("");
    logRecord.setSourceMethodName("");
    return logRecord;
  }

  private static synchronized Handler getHandler() {
    if (ourHandler != null)
      return ourHandler;
    if (!ENABLED)
      return null;
    if (ourLogDirectory == null)
      return null;
    if (!ourLogDirectory.isDirectory())
      return null;
    IOException ioException = null;
    try {
      ourHandler = new FileHandler(new File(ourLogDirectory, LOGFILE_PATTERN).getAbsolutePath(), LIMIT, COUNT, true);
    } catch (IOException e) {
      // cannot log to file!
      ourHandler = new ConsoleHandler();
      ioException = e;
    }
    setupHandler(ourHandler);
    if (ioException != null)
      ourHandler.publish(createLogRecord(Level.WARNING, "cannot log to file", ioException));
    return ourHandler;
  }

  private static boolean log(Level level, String message, Throwable e) {
    Handler handler = getHandler();
    if (handler != null)
      handler.publish(createLogRecord(level, message, e));
    return true;
  }

  private static void setupHandler(Handler handler) {
    handler.setLevel(Level.ALL);
    handler.setFormatter(new DecentFormatter());
  }
}
