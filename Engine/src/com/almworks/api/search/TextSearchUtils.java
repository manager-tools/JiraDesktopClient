package com.almworks.api.search;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.engine.Connection;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TextSearchUtils {
  public static final String WORD_DELIMITERS = "[,; \t]+";

  private TextSearchUtils() {
  }

  public static boolean isDelimiter(char c) {
    return ",; \t".indexOf(c) >= 0;
  }

  public static Collection<GenericNode> escalateToConnectionNodes(Collection<? extends GenericNode> nodes) {
    if (nodes == null)
      return null;
    if (nodes.size() == 0)
      return Collections15.emptyCollection();
    Set<GenericNode> result = Collections15.linkedHashSet();
    for (GenericNode node : nodes) {
      ConnectionNode connection = node.getAncestorOfType(ConnectionNode.class);
      if (connection == null) {
        // if any node is not under connection, search globally
        RootNode root = node.getRoot();
        if (root == null)
          return Collections15.emptySet();
        else
          return Collections.<GenericNode>singleton(root);
      }
      result.add(connection);
    }
    return result;
  }

  public static Collection<Connection> getAffectedConnections(Collection<GenericNode> nodes) {
    LinkedHashSet<Connection> result = Collections15.linkedHashSet();
    for (GenericNode node : nodes) {
      ConnectionNode cnode = node.getAncestorOfType(ConnectionNode.class);
      if (cnode != null) {
        result.add(cnode.getConnection());
      } else {
        RootNode root = node.getRoot();
        if (root != null) {
          for (GenericNode child : root.getChildren()) {
            if (child instanceof ConnectionNode) {
              result.add(child.getConnection());
            }
          }
          break;
        }
      }
    }
    return result;
  }
}
