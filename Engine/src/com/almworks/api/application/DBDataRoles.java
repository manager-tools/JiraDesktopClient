package com.almworks.api.application;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public abstract class DBDataRoles {
  public static final DataRole<Long> ITEM_ROLE = DataRole.createRole(Long.class);

  @NotNull
  public static Connection findConnection(ActionContext context) throws CantPerformException {
    try {
      return context.getSourceObject(ConnectionNode.CONNECTION_NODE).getConnection();
    } catch (CantPerformException e) {
      try {
        return context.getSourceObject(ConnectionNode.CONNECTION);
      } catch (CantPerformException e1) {
        List<ATreeNode<GenericNode>> nodes =
          GenericNode.GET_TREE_NODE.collectList(context.getSourceCollection(GenericNode.NAVIGATION_NODE));
        Connection connection = lookForConnectionInAncestors(TreeUtil.commonParent(nodes));
        if (connection != null)
          return connection;

        Engine engine = context.getSourceObject(Engine.ROLE);
        Collection<Connection> connections = engine.getConnectionManager().getReadyConnectionsModel().copyCurrent();
        if (connections.size() == 1) {
          return connections.iterator().next();
        }

        throw new CantPerformException(e);
      }
    }
  }

  @Nullable
  public static Connection lookForConnectionInAncestors(ATreeNode node) {
    while (node != null) {
      Object userObject = node.getUserObject();
      if (userObject instanceof ConnectionNode)
        return ((ConnectionNode) userObject).getConnection();
      node = node.getParent();
    }
    return null;
  }

  public static boolean isAnyConnectionAllowsUpload(ActionContext context) throws CantPerformException {
    ConnectionManager manager = context.getSourceObject(Engine.ROLE).getConnectionManager();
    List<Connection> connections = manager.getConnections().copyCurrent();
    for (Connection connection : connections) if (connection.isUploadAllowed()) return true;
    return false;
  }
}
