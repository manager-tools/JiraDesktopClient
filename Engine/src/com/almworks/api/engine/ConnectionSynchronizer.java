package com.almworks.api.engine;

import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ConnectionSynchronizer extends SyncTask {
  Role<ConnectionSynchronizer> ROLE = Role.role(ConnectionSynchronizer.class);

  void synchronize(SyncParameters parameters);
}
