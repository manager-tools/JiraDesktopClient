package com.almworks.util.fx.test;

import com.almworks.util.LogHelper;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.sun.javafx.application.PlatformImpl;
import util.concurrent.SynchronizedBoolean;

public abstract class FXTestCase extends BaseTestCase {
  private static final SynchronizedBoolean ourStarted = new SynchronizedBoolean(false);

  {
    try {
      initialize();
    } catch (InterruptedException e) {
      LogHelper.error(e);
      throw new RuntimeException();
    }
  }
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public static void initialize() throws InterruptedException {
    if (ourStarted.get()) return;
    MockToolkit.install();
    PlatformImpl.startup(() -> ourStarted.set(true));
    ourStarted.waitForValue(true);
  }

  public static void flushQueue() {
    flushQueue(1);
  }

  public static void flushQueue(int count) {
    for (int i = 0; i < count; i++) PlatformImpl.runAndWait(() -> {});
  }

  public static abstract class FXThread extends FXTestCase {
    public FXThread() {
      setGate(ThreadGate.FX_IMMEDIATE);
    }
  }
}
