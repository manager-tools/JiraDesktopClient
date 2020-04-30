package com.almworks.api.application.tree;

import com.almworks.api.engine.Connection;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConnectionNode extends GenericNode, RenamableNode {
  DataRole<ConnectionNode> CONNECTION_NODE = DataRole.createRole(ConnectionNode.class);
  DataRole<Connection> CONNECTION = DataRole.createRole(Connection.class);

  @NotNull
  Connection getConnection();

  String getConnectionName();

  boolean isConnectionReady();

  GenericNode findTemporaryFolder();

  @Nullable
  GenericNode getOutboxNode();
}
