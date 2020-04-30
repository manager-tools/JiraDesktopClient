package com.almworks.util.fx.layout;

import javafx.scene.Node;

public class LayoutConstraint<T> {
  private final String myName;

  public LayoutConstraint(String name) {
    myName = name;
  }

  public void set(Node node, T value) {
    if (value == null) {
      node.getProperties().remove(myName);
    } else {
      node.getProperties().put(myName, value);
    }
    if (node.getParent() != null) {
      node.getParent().requestLayout();
    }
  }

  @SuppressWarnings("unchecked")
  public T get(Node node) {
    return (T) node.getProperties().get(myName);
  }
}
