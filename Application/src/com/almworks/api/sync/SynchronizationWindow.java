package com.almworks.api.sync;

import com.almworks.api.engine.SyncProblem;
import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface SynchronizationWindow {
  Role<SynchronizationWindow> ROLE = Role.role("SynchronizationWindow");

  void show();

  void showProblem(SyncProblem problem);
}
