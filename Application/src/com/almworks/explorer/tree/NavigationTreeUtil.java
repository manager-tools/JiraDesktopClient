package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.collections.BreakVisitingException;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.collections.TracingElementVisitor;
import com.almworks.util.threads.ThreadAWT;

import java.util.List;

class NavigationTreeUtil {
  @ThreadAWT
  static <T> boolean visitTree(GenericNode node, Class<T> nodeClass, boolean childrenFirst, ElementVisitor<T> visitor) {
    assert nodeClass != null;
    if (node == null)
      return true;
    boolean result = true;
    if (!childrenFirst && (nodeClass.isInstance(node))) {
      result = visitor.visit((T) node);
      if (!result)
        return result;
    }
    int count = node.getChildrenCount();
    for (int i = 0; i < count; i++) {
      GenericNode child = node.getChildAt(i);
      result = visitTree(child, nodeClass, childrenFirst, visitor);
      if (!result)
        return result;
    }
    if (childrenFirst && (nodeClass.isInstance(node))) {
      result = visitor.visit((T) node);
    }
    return result;
  }

  /**
   * The order is Root-Left-Right alwayss
   */
  static <T, D> void traceTree(GenericNode node, Class<T> nodeClass, D initialData, TracingElementVisitor<T, D> visitor)
    throws BreakVisitingException
  {
    assert nodeClass != null;
    if (node == null)
      return;
    D data = initialData;
    if (nodeClass.isInstance(node)) {
      data = visitor.visit((T) node, data);
    }
    List<? extends GenericNode> children = node.getChildren();
    for (GenericNode child : children) {
      traceTree(child, nodeClass, data, visitor);
    }
  }

  public static int askParentToCompareNodes(GenericNode node1, GenericNode node2) {
    if (node1 == node2)
      return 0;
    GenericNode p1 = node1.getParent();
    GenericNode p2 = node2.getParent();
    GenericNode parent = p1 == null ? p2 : p1;
    if (parent != null && (!node1.isNode() || !node2.isNode() || p1 == p2)) {
      // if nodes have same parent (or one node is not in the model)
        return parent.compareChildren(node1, node2);
    } else {
      // cannot say anything about nodes that are not children of a same parent - use weight
      return compareNodes(node1, node2);
    }
  }

  public static int compareNodes(GenericNode node1, GenericNode node2) {
    int diff = ViewWeightManager.compare(node1, node2);
    if (diff != 0)
      return diff;
    return node1.compareToSame(node2);
  }
}
