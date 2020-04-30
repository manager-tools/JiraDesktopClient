package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A common class for logging.
 */
public final class Log {
  private static final String DEFAULT_APPLICATION_LOGGER_NAME = "org.almworks";

  private static String ourApplicationLoggerName;
  private static Logger ourApplicationLogger;

  private static boolean ourErrorOccurred = false;
  private static volatile Runnable ourOutOfMemoryHandler = null;

  public static void error(Object object, Throwable throwable) {
    error(object, throwable, null);
  }

  private static void error(Object object, Throwable throwable, Object[] params) {
    final String message = formatMessage(object);
    ourErrorOccurred = true;
    if (throwable == null) throwable = new Throwable(message);
    final LogRecord record = createLogRecord(Level.SEVERE, message, throwable);
    record.setParameters(params);
    getApplicationLogger().log(record);

    if(isOutOfMemory(throwable)) {
      onOutOfMemory();
    }
  }

  private static String formatMessage(Object object) {
    if (object == null) {
      return "";
    } else if (object instanceof String) {
      return (String) object;
    } else {
      return object.toString() + " (class " + object.getClass().getName() + ") caused error";
    }
  }

  public static void error(String message) {
    error(message, new Throwable(message));
  }

  public static void error(Throwable e) {
    error(null, e);
  }

  public static void warn(String message) {
    warn(message, null);
  }

  public static void warn(String message, Throwable e) {
    getApplicationLogger().log(createLogRecord(Level.WARNING, message, e));
  }

  public static void warn(Throwable e) {
    warn("", e);
  }

  public static boolean debug(String message) {
    return debug(message, null);
  }

  public static boolean debug(String message, Throwable throwable) {
    getApplicationLogger().log(createLogRecord(Level.INFO, message, throwable));
    return true;
  }

  public static boolean debug(Throwable e) {
    return debug(null, e);
  }

  public static synchronized Logger getRootLogger() {
    return Logger.getLogger("");
  }

  public static synchronized Logger getApplicationLogger() {
    if (ourApplicationLogger == null) {
      if (ourApplicationLoggerName == null) {
        ourApplicationLoggerName = DEFAULT_APPLICATION_LOGGER_NAME;
      }
      ourApplicationLogger = Logger.getLogger(DEFAULT_APPLICATION_LOGGER_NAME);
    }
    return ourApplicationLogger;
  }

  public static void installOutOfMemoryHandler(Runnable runnable) {
    ourOutOfMemoryHandler = runnable;
  }

  public static void setApplicationLoggerName(String name) {
    ourApplicationLoggerName = name;
    ourApplicationLogger = null;
  }

  public static boolean isExceptionOccured() {
    return ourErrorOccurred;
  }

  public static boolean isOutOfMemory(Throwable throwable) {
    while (throwable != null) {
      if (throwable instanceof OutOfMemoryError)
        return true;
      throwable = throwable.getCause();
    }
    return false;
  }

  public static Handler[] replaceHandlers(Logger logger, Handler handler) {
    return replaceHandlers(logger, new Handler[] {handler});
  }

  public static Handler[] replaceHandlers(@NotNull Logger logger, @Nullable Handler[] handlers) {
    Handler[] oldHandlers = logger.getHandlers();
    for (Handler oldHandler : oldHandlers)
      logger.removeHandler(oldHandler);
    if (handlers != null) {
      for (Handler handler : handlers)
        logger.addHandler(handler);
    }
    return oldHandlers;
  }

  public static boolean hasHandlerOfClass(Class<? extends Handler> handlerClass) {
    Handler[] handlers = getRootLogger().getHandlers();
    for (Handler handler : handlers)
      if (handlerClass.isAssignableFrom(handler.getClass()))
        return true;
    return false;
  }

  private static LogRecord createLogRecord(Level level, String message, Throwable throwable) {
    LogRecord logRecord = new LogRecord(level, message);
    logRecord.setThrown(throwable);
    logRecord.setSourceClassName("");
    logRecord.setSourceMethodName("");
    return logRecord;
  }

  private static void onOutOfMemory() {
    Runnable handler = ourOutOfMemoryHandler;
    if (handler != null) {
      handler.run();
    }
  }

  private Log() {}
}
