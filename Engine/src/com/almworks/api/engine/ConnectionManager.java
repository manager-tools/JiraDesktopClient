package com.almworks.api.engine;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

public interface ConnectionManager {
  @ThreadAWT
  @Nullable
  Connection createConnection(ItemProvider provider, ReadonlyConfiguration configuration)
    throws ConfigurationException, ProviderDisabledException;

  Connection getConnection(String connectionID);

  String getConnectionName(String connectionID);

  CollectionModel<Connection> getConnections();

  Collection<ItemProvider> getProviders();

  void removeConnection(Connection connection);

  void updateConnection(Connection connection, ReadonlyConfiguration configuration);

  /**
   * May throw RuntimeException if connection is not ready :(
   * @param connectionItem
   */
  @Nullable
  Connection findByItem(long connectionItem);

  /**
   * Waits for a connection to become ready, if necessary.
   * @param ifReady if true returns connection only if it is in READY state. False means that caller code assumes that connection has to be in READY state, if the assumption fails returns null and log notification message
   */
  @Nullable
  Connection findByItem(long connectionItem, boolean wait, boolean ifReady);

  Detach addConnectionChangeListener(ThreadGate callBackGate, ConnectionChangeListener listener);

  CollectionModel<Connection> getReadyConnectionsModel();

  @ThreadAWT
  Connection getConnectionForUrl(String itemUrl);

  @ThreadAWT
  ItemProvider getProviderForUrl(String url);

  @ThreadSafe
  void waitUntilLoaded() throws InterruptedException;

  void whenConnectionsLoaded(Lifespan life, ThreadGate gate, Runnable runnable);

  Modifiable getConnectionsModifiable();

  void fireConnectionsModifiable();

  class ConnectionNameOrder implements Comparator<Connection> {
    private final ConnectionManager myManager;

    public ConnectionNameOrder(ConnectionManager manager) {
      myManager = manager;
    }

    public int compare(Connection c1, Connection c2) {
      String name1 = getName(c1);
      String name2 = getName(c2);
      if (name1 == name2) return 0;
      if (name1 == null || name2 == null) return name1 == null ? -1 : 1;
      return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    private String getName(Connection connection) {
      return connection != null ? myManager.getConnectionName(connection.getConnectionID()) : null;
    }
  }
}

