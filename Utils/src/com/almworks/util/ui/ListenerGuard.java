package com.almworks.util.ui;

import com.almworks.util.threads.Threads;

/**
 * Helper class for AWT event listeners reentrancy handling.
 */
public class ListenerGuard {
  private boolean myGuarded = false;

  public boolean acquire() {
    Threads.assertAWTThread();
    if (myGuarded)
      return false;
    myGuarded = true;
    return true;
  }

  public void release() {
    Threads.assertAWTThread();
    assert myGuarded;
    myGuarded = false;
  }
}
