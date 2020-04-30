package com.almworks.util;

import org.apache.commons.logging.impl.Jdk14Logger;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Vasya
 */
public class ApacheLogger extends Jdk14Logger {
  public ApacheLogger(String name) {
    super(name);
  }

  public boolean isDebugEnabled() {
    return super.isDebugEnabled();
  }
                            
  /**
   * "No stacktrace" overriding
   */
  private void log(Level level, Object msg, Throwable ex) {
    Logger logger = getLogger();

    // See http://bugzilla/main/show_bug.cgi?id=838
    // The problem is that Apache would sometimes log an error, which may cause
    // error dialog to appear. We have to decrease severity.
    if (level.intValue() > Level.WARNING.intValue()) {
      msg = "[Apache:" + level.toString() + "] " + String.valueOf(msg);
      level = Level.WARNING;
    }

    if (!logger.isLoggable(level))
      return;

    if (ex == null)
      logger.logp(level, null, null, String.valueOf(msg));
    else
      logger.logp(level, null, null, String.valueOf(msg), ex);
  }

  //log() callers
  public final void trace(Object message, Throwable exception) {
    log(Level.FINEST, message, exception);
  }

  public final void debug(Object message, Throwable exception) {
    log(Level.FINE, message, exception);
  }

  public final void info(Object message, Throwable exception) {
    log(Level.INFO, message, exception);
  }

  public final void warn(Object message, Throwable exception) {
    log(Level.WARNING, message, exception);
  }

  public final void error(Object message, Throwable exception) {
    log(Level.SEVERE, message, exception);
  }

  public final void fatal(Object message, Throwable exception) {
    log(Level.SEVERE, message, exception);
  }

  // redirects of invocations
  public final void trace(Object message) {
    trace(message, null);
  }

  public final void debug(Object message) {
    debug(message, null);
  }

  public final void info(Object message) {
    info(message, null);
  }

  public final void warn(Object message) {
    warn(message, null);
  }

  public final void error(Object message) {
    error(message, null);
  }

  public final void fatal(Object message) {
    fatal(message, null);
  }

}
