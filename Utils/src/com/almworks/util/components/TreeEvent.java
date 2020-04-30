package com.almworks.util.components;

import java.util.List;

public class TreeEvent<T> {
  private final TreeModelBridge<T> mySource;

  public TreeEvent(TreeModelBridge<T> source) {
    mySource = source;
  }

  public static <T> TreeEvent<T> nodeChanged(TreeModelBridge<T> node) {
    return new TreeEvent<T>(node);
  }

  public static <T> TreeEvent<T> nodeInserted(TreeModelBridge<T> parent, TreeModelBridge<? extends T> child) {
    return new TreeEvent<T>(parent);
  }

  public static <T> TreeEvent<T> nodeRemoved(TreeModelBridge<T> parent, TreeModelBridge<? extends T> child) {
    return new TreeEvent<T>(parent);
  }

  public static <T> TreeEvent<T> nodesRemoved(TreeModelBridge<T> parent, List<? extends TreeModelBridge<? extends T>> nodes) {
    return new TreeEvent<T>(parent);
  }

  public TreeModelBridge<T> getSource() {
    return mySource;
  }
}
