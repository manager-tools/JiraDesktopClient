package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ThreadGateProvider {
  ThreadGate getThreadGate(Object context);
}
