package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.Collections15;

import java.awt.*;

class Node {
  @SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
  private static final Log<Node> log = Log.get(Node.class);
  private final NodeRoot myRoot;
  private final Node myParent;
  private final Rectangle myRect = new Rectangle();
  private final java.util.List<Node> myNodes = Collections15.arrayList();
  private final SimpleModifiable mySelectedModifable = new SimpleModifiable();
  private final SimpleModifiable myLayoutModifiable = new SimpleModifiable();

  Node(NodeRoot root, Node parent) {
    myRoot = root;
    myParent = parent;
  }

  public boolean isRoot() {
    return myParent == null;
  }

  public int getCornerX() {
    return myRect.x + myRect.width;
  }

  public int getCornerY() {
    return myRect.y + myRect.height;
  }

  public int getCorner(boolean x) {
    return x ? getCornerX() : getCornerY();
  }

  public int getOrigin(boolean x) {
    return x ? myRect.x : myRect.y;
  }

  public void getSize(int[] size) {
    int width = myRect.width;
    int height = myRect.height;
    for (Node node : myNodes) {
      width = Math.max(width, node.getCornerX());
      height = Math.max(height, node.getCornerY());
    }
    size[0] = width;
    size[1] = height;
  }

  public boolean isEmpty() {
    return myNodes.isEmpty();
  }

  public Modifiable getSelectedModifable() {
    return mySelectedModifable;
  }

  public SimpleModifiable getLayoutModifiable() {
    return myLayoutModifiable;
  }

  public boolean isSelected() {
    return myRoot.getSelected() == this;
  }

  public NodeRoot getRoot() {
    return myRoot;
  }

  public void fireSelectedChanged() {
    mySelectedModifable.fireChanged();
  }

  public Node getChild(int index) {
    return myNodes.get(index);
  }

  public int getChildCount() {
    return myNodes.size();
  }

  public int getWidth() {
    return myRect.width;
  }

  public int getHeight() {
    return myRect.height;
  }

  public void addChild(int x, int y, int width, int height) {
    if (width <= 0 || height <= 0) return;
    Node child = new Node(myRoot, this);
    child.myRect.setBounds(x, y, width, height);
    myNodes.add(child);
    myLayoutModifiable.fireChanged();
  }

  public void insertChild(int x, int y, int maxWidth, int maxHeight) {
    int minCX = maxWidth;
    int minCY = maxHeight;
    for (Node node : myNodes) {
      if (node.myRect.contains(x, y)) {
        log.error(this, "Inside child", node.myRect, x, y);
        return;
      }
      minCX = updateMinCorner(x, y, minCX, node, true);
      minCY = updateMinCorner(y, x, minCY, node, false);
    }
    int width = minCX - x;
    int height = minCY - y;
    width = Math.min(width, getWidth() / 2);
    height = Math.min(height, getHeight() / 2);
    addChild(x, y, width, height);
  }

  private int updateMinCorner(int x, int y, int min, Node node, boolean xAxis) {
    if (y >= node.getOrigin(!xAxis) && y < node.getCorner(!xAxis) && x < node.getOrigin(xAxis))
      min = Math.min(min, node.getOrigin(xAxis));
    return min;
  }

  public void setSize(int width, int height) {
    if (width != getWidth() || height != getHeight()) {
      myRect.setSize(width, height);
      myLayoutModifiable.fireChanged();
    }
  }

  public void move(int dx, int dy) {
    if (myParent == null) return;
    Rectangle newRect = new Rectangle(myRect);
    newRect.x += dx;
    newRect.y += dy;
    if (!checkNewBounds(newRect)) return;
    myRect.setBounds(newRect);
    myParent.myLayoutModifiable.fireChanged();
  }

  public void resize(int dx, int dy) {
    if (myParent == null) return;
    Rectangle newRect = new Rectangle(myRect);
    newRect.width += dx;
    newRect.height += dy;
    if (!checkNewBounds(newRect)) return;
    myRect.setBounds(newRect);
    myParent.myLayoutModifiable.fireChanged();
  }

  private boolean checkNewBounds(Rectangle bounds) {
    for (Node node : myParent.myNodes) {
      if (node == this) continue;
      if (bounds.intersects(node.myRect)) return false;
    }
    return true;
  }

  static class NodeRoot {
    private final Node myRoot = new Node(this, null);
    private Node mySelection = myRoot;

    public Node getSelected() {
      return mySelection;
    }

    public void setSelected(Node node) {
      if (mySelection == node || node == null) return;
      Node oldSelection = mySelection;
      mySelection = node;
      oldSelection.fireSelectedChanged();
      node.fireSelectedChanged();
    }
  }
}
