package com.almworks.util.exec;

/**
 * Provides the environment parameters for LongEventQueue
 */
public class LongEventQueueEnv {
  public long getNormalTaskDuration() {
    return 700;
  }

  public int getMaxThreads() {
    return 20;
  }

  public long getDebugStatPeriod() {
    return 30000;
  }

  public long getMinIntervalBetweenWarnings() {
    return 5000;
  }

  public boolean getDebug() {
    return false;
  }

  public boolean getSingleThread() {
    return false;
  }
}
