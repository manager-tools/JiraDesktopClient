package com.almworks.explorer.tree;

import com.almworks.api.application.tree.ConnectionNode;

public interface ConnectionAwareNode {
  /**
   * is invoked when connection becomes ready or becomes not ready
   */
  void onConnectionState(ConnectionNode connection, boolean ready);
}
