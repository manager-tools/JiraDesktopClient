package com.almworks.util.fx.test;

import com.almworks.util.LocalLog;
import com.sun.javafx.perf.PerformanceTracker;

public class MockPerformanceTracker extends PerformanceTracker {
  private final LocalLog myLog = LocalLog.topLevel("FXmock");

  @Override
  protected long nanoTime() {
    return System.nanoTime();
  }

  @Override
  public void doOutputLog() {
  }

  @Override
  public void doLogEvent(String s) {
    myLog.debug(s);
  }
}
