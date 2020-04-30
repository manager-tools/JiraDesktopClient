package com.almworks.api.syncreg;

import com.almworks.api.engine.Connection;
import com.almworks.util.events.EventSource;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

public interface SyncRegistry {
  public static final Role<SyncRegistry> ROLE = Role.role("syncreg");

  EventSource<Listener> getEventSource();

  SyncFlagRegistry getSyncFlagRegistry();

  @NotNull
  SyncCubeRegistry getSyncCubeRegistry();

  void clearRegistryForConnection(Connection connection);

  void lockUpdate();

  void unlockUpdate();

  ScalarModel<Boolean> getStartedModel();

  interface Listener {
    void onSyncRegistryChanged(boolean moreSynchronized, boolean lessSynchronized);
  }
}
