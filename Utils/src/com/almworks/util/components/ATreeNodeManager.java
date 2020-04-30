package com.almworks.util.components;

import org.jetbrains.annotations.Nullable;

public interface ATreeNodeManager<T> {
  /**
   * In a chain of filtered tree models, returns a node that corresponds to the given node.
   * Node X from tree T1 is corresponding to node Y from T2 if
   * - if T1 and T2 are the same, and X and Y are the same
   * - if T2 is "below" T1 (meaning that nodes of T1 somehow reflect some nodes of T2), and Y is mapped to X
   * Returns null if no corresponding node is found.
   */
  @Nullable ATreeNode<T> findCorresponding(ATreeNode<T> subject);
}
