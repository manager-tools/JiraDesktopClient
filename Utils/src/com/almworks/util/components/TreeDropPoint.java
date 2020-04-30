package com.almworks.util.components;

import org.jetbrains.annotations.NotNull;

public class TreeDropPoint {
  private final Object myNode;
  private final boolean myInsertNode;
  private final int myInsertionIndex;

  private TreeDropPoint(@NotNull Object node, boolean insertNode, int insertionIndex) {
    myNode = node;
    myInsertNode = insertNode;
    myInsertionIndex = insertionIndex;
  }

  public static TreeDropPoint insert(Object node, int index) {
    assert index >= 0;
    return new TreeDropPoint(node, true, index);
  }

  public static TreeDropPoint consume(Object node) {
    return new TreeDropPoint(node, false, -1);
  }

  public Object getNode() {
    return myNode;
  }

  public boolean isInsertNode() {
    return myInsertNode;
  }

  public int getInsertionIndex() {
    return myInsertionIndex;
  }

  public String toString() {
    return myInsertNode ? "TDP[" + myNode + " ins " + myInsertionIndex + "]" : "TDP[" + myNode + " eat]";
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TreeDropPoint that = (TreeDropPoint) o;

    if (myInsertNode != that.myInsertNode)
      return false;
    if (myInsertionIndex != that.myInsertionIndex)
      return false;
    if (myNode != null ? !myNode.equals(that.myNode) : that.myNode != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myNode != null ? myNode.hashCode() : 0);
    result = 31 * result + (myInsertNode ? 1 : 0);
    result = 31 * result + myInsertionIndex;
    return result;
  }
}
