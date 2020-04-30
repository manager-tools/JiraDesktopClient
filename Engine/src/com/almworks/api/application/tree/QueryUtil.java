package com.almworks.api.application.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.items.api.DBFilter;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QueryUtil {
  @Nullable
  public static DBFilter maybeGetHintedView(GenericNode node) {
    return maybeGetHintedView(node, null);
  }

  @ThreadAWT
  @Nullable
  public static DBFilter maybeGetHintedView(GenericNode node, QueryResult result) {
    if (node == null) {
      // cannot get hint without node
      return result == null ? null : result.getDbFilter();
    }
    if (result == null) {
      result = node.getQueryResult();
    } else {
      QueryResult nodeResult = node.getQueryResult();
      if (nodeResult != result) {
        assert false : node + " " + nodeResult + " " + result;
        Log.warn("bad node/result: " + node + " " + nodeResult + " " + result);
        return result.getDbFilter();
      }
    }
    return result.getDbFilter();
  }

  @Nullable
  public static ResolvedItem getOneResolvedItem(@Nullable ItemKey key,
    @Nullable ConstraintDescriptor descriptor, @Nullable ItemHypercube hypercube)
  {
    if (key == null)
      return null;
    if (key instanceof ResolvedItem)
      return (ResolvedItem) key;
    if (descriptor == null)
      return null;
    ConstraintType type = descriptor.getType();
    if (!(type instanceof EnumConstraintType))
      return null;
    EnumConstraintType enumType = ((EnumConstraintType) type);
    if (hypercube == null)
      hypercube = new ItemHypercubeImpl();
    List<ResolvedItem> resolution = enumType.resolveKey(key.getId(), hypercube);
    return resolution == null || resolution.size() == 0 ? null : resolution.get(0);
  }

  @Nullable
  public static ConnectionNode findConnectionNode(ExplorerComponent explorer, Connection connection) {
    RootNode root = explorer.getRootNode();
    if (root == null) return null;
    for (int i = 0; i < root.getChildrenCount(); i++) {
      GenericNode child = root.getChildAt(i);
      if (child instanceof ConnectionNode && connection.equals(child.getConnection())) return (ConnectionNode) child;
    }
    return null;
  }
}
