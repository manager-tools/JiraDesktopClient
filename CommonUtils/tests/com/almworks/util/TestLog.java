package com.almworks.util;

import junit.framework.Assert;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author : Dyoma
 */
public class TestLog extends Handler {
  private static TestLog myInstance = null;
  private static Formatter myFormatter = new DecentFormatter();

  private final List<RuntimeException> myLog = Collections15.arrayList();
  private boolean myWriteToStdout = false;
  private Level myStdoutLevel = Level.INFO;
  private Level myTestFailThreshold = Level.WARNING;

  public void setTestFailThreshold(Level testFailThreshold) {
    Assert.assertNotNull(testFailThreshold);
    myTestFailThreshold = testFailThreshold;
  }

  public void close() throws SecurityException {
  }

  public void flush() {
  }

  public synchronized void publish(LogRecord record) {
    int level = record.getLevel().intValue();
    if (level > myTestFailThreshold.intValue()) {
      RuntimeException exception = new RuntimeException(record.getMessage(), record.getThrown());
      myLog.add(exception);
    }
    if (myWriteToStdout && level >= myStdoutLevel.intValue()) {
      System.out.print(myFormatter.format(record));
    }
  }

  public void setWriteToStdout(boolean writeToStdout) {
    myWriteToStdout = writeToStdout;
  }

  public static void install() {
    if (Log.hasHandlerOfClass(TestLog.class))
      return;
    myInstance = new TestLog();
    Log.replaceHandlers(Log.getRootLogger(), myInstance);
    Log.getRootLogger().setLevel(Level.FINE);
  }

  public static TestLog getInstance() {
    return myInstance;
  }

  public synchronized void clearLog() {
    myLog.clear();
  }

  public synchronized boolean printLog() {
    int size = myLog.size();
    if (size == 0)
      return false;
    RuntimeException[] exceptions = myLog.toArray(new RuntimeException[size]);
    for (int i = 0; i < size; i++) {
      Throwable cause = exceptions[i];
      while (cause.getCause() != null)
        cause = cause.getCause();
      if (cause != exceptions[i])
        cause.printStackTrace();
      exceptions[i].printStackTrace();
    }
    return true;
  }

  public void setStdoutLevel(Level stdoutLevel) {
    myStdoutLevel = stdoutLevel;
  }
}
