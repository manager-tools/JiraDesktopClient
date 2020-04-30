package com.almworks.util;

import java.util.logging.Level;

public class LocalLog {
  private final LocalLog myParent;
  private final String myName;
  private final int myDepth;

  private LocalLog(LocalLog parent, String name, int depth) {
    myParent = parent;
    myName = "[" + name + "]";
    myDepth = depth;
  }

  public static LocalLog topLevel(String name) {
    return new LocalLog(null, name, 1);
  }

  public LocalLog child(String name) {
    return new LocalLog(this, name, myDepth + 1);
  }

  public void log(Level level, Object... message) {
    if (!isLoggable(level)) return;
    Object[] fullRecord;
    if (message != null && message.length > 0) {
      fullRecord = new Object[myDepth + message.length];
      System.arraycopy(message, 0, fullRecord, myDepth, message.length);
    } else fullRecord = new Object[myDepth];
    sendLogRecord(level, fullRecord);
  }

  public boolean isLoggable(Level level) {
    return LogHelper.isLoggable(level);
  }

  private void sendLogRecord(Level level, Object[] fullRecord) {
    fullRecord[myDepth - 1] = myName;
    if (myParent != null) myParent.sendLogRecord(level, fullRecord);
    else LogHelper.log(level, fullRecord);
  }

  public void debug(Object ... message) {
    log(Level.INFO, message);
  }

  public void fine(Object ... message) {
    log(Level.FINE, message);
  }

  public void warning(Object ... message) {
    log(Level.WARNING, message);
  }

  public void error(Object ... message) {
    log(Level.SEVERE, message);
  }

  /** @return true so that this method can be used only when assertions are on */
  public boolean assertError(boolean condition, Object ... messages) {
    if (!condition) error(messages);
    return true;
  }
}
