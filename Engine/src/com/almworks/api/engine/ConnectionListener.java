package com.almworks.api.engine;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ConnectionListener {
  void onSynchronizationStarted(Connection connection);

  void onSynchronizationFinished(Connection connection, boolean success);
}
