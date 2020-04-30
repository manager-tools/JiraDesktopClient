package com.almworks.util.threads;

import org.almworks.util.ExceptionUtil;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class Job implements Runnable {
  abstract void perform() throws Exception;

  public void run() {
    try {
      perform();
    } catch (Exception e) {
      throw ExceptionUtil.wrapUnchecked(e);
    }
  }
}
