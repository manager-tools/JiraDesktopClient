package com.almworks.api.application;

import com.almworks.api.application.tree.GenericNode;
import org.almworks.util.Util;

public class NodeTabKey implements TabKey {
  private GenericNode myNode;

  public NodeTabKey(GenericNode node) {
    myNode = node;
  }

  public final boolean isReplaceTab(TabKey tabKey) {
    if (!(tabKey instanceof NodeTabKey))
      return false;
    NodeTabKey other = (NodeTabKey) tabKey;
    if (!Util.equals(myNode, other.getNode()))
      return false;
    return isReplaceNodeTab(other);
  }

  protected boolean isReplaceNodeTab(NodeTabKey nodeKey) {
    return false;
  }

  protected final GenericNode getNode() {
    return myNode;
  }

  public int hashCode() {
    return myNode.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof NodeTabKey))
      return false;
    return myNode.equals(((NodeTabKey) obj).myNode);
  }
}
