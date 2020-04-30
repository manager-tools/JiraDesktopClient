package com.almworks.util.components.tables;

import com.almworks.util.components.ObjectWrapper;
import com.almworks.util.ui.TreeUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Arrays;

/**
 * @author dyoma
 */
public class TreeNodeState {
  private final Rectangle myBounds;
  private final TreePath myPath;
  @Nullable
  private final Object myFirstChild;
  private final boolean myExpanded;
  private final boolean myLeadSelection;
  private final boolean[] myLastNode;
  private final boolean myShowRootHandlers;
  private final boolean myTopRoot;
  private final boolean myTopMiddle;

  public TreeNodeState(TreePath path, Rectangle bounds, @Nullable Object firstChild, boolean expanded,
    boolean leadSelection, boolean[] lastNode, boolean showRootHandlers, boolean topMiddle, boolean topRoot) {
    myPath = path;
    myBounds = bounds;
    myFirstChild = firstChild;
    myExpanded = expanded;
    myLeadSelection = leadSelection;
    myShowRootHandlers = showRootHandlers;
    myTopRoot = topRoot;
    assert lastNode[0];
    assert path.getPathCount() == lastNode.length;
    myLastNode = lastNode;
    myTopMiddle = topMiddle;
  }

  public Dimension getCellSize() {
    return myBounds.getSize();
  }

  public int getX() {
    return myBounds.x;
  }

  public int getY() {
    return myBounds.y;
  }

  public boolean isExpanded() {
    return myExpanded;
  }

  public TreePath setupNode(DefaultTreeModel model, JTree tree) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    ensureHasChildren(model, root);
    DefaultMutableTreeNode ownNode = (DefaultMutableTreeNode) root.getChildAt(0);
    return setupLastNode(ownNode, model, tree);
  }

  private TreePath setupLastNode(DefaultMutableTreeNode ownNode, DefaultTreeModel model, JTree tree) {
    TreePath path = TreeUtil.pathFromRoot(ownNode);
    TreePath parentPath = path.getParentPath();
    if (parentPath != null)
      tree.expandPath(parentPath);
    if (isLeaf()) {
      ownNode.removeAllChildren();
      model.nodeStructureChanged(ownNode);
    } else {
      ensureHasChildren(model, ownNode);
      if (myExpanded)
        tree.expandPath(path);
      else
        tree.collapsePath(path);
    }
    if (myLeadSelection)
      tree.setSelectionPath(path);
    else
      tree.clearSelection();
    return path;
  }

  private boolean isLeaf() {
    return myFirstChild == null;
  }

  private void ensureHasChildren(DefaultTreeModel model, MutableTreeNode node) {
    if (node.getChildCount() > 0)
      return;
    model.insertNodeInto(new DefaultMutableTreeNode(null), node, 0);
  }

  @Nullable
  public TreePath restorePath(TreePath mockPath, TreePath targetPath) {
    DefaultMutableTreeNode mockNode = (DefaultMutableTreeNode) mockPath.getLastPathComponent();
    DefaultMutableTreeNode ownNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
    if (mockNode == ownNode)
      return myPath;
    if (ownNode == mockNode.getParent())
      return myPath.pathByAddingChild(myFirstChild);
    if (mockNode == ownNode.getParent())
      return myPath.getParentPath();
    return null;
  }

  public Object[] getPathComponents() {
    return myPath.getPath();
  }

  public TreePath setupPath(DefaultTreeModel model, JTree tree) {
    tree.setShowsRootHandles(myShowRootHandlers);
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) model.getRoot();
    if (myPath.getPathCount() == 1) {
      parent.setUserObject(myPath.getLastPathComponent());
      return setupLastNode(parent, model, tree);
    }
    Object[] components = myPath.getPath();
    parent.setUserObject(components[0]);
    for (int i = 1; i < components.length - 1; i++) {
      Object component = components[i];
      boolean isLast = myLastNode[i];
      parent = getChildNode(parent, isLast, component, model);
    }
    tree.expandPath(TreeUtil.pathFromRoot(parent));
    DefaultMutableTreeNode node = getChildNode(parent, myLastNode[myLastNode.length - 1], components[components.length - 1], model);
    return setupLastNode(node, model, tree);
  }

  private DefaultMutableTreeNode getChildNode(MutableTreeNode parent, boolean last, Object component, DefaultTreeModel model) {
    int childCount = parent.getChildCount();
    if (childCount == 0) {
      model.insertNodeInto(new DefaultMutableTreeNode(null), parent, 0);
      childCount++;
    }
    DefaultMutableTreeNode result;
    if (last) {
      result = (DefaultMutableTreeNode) parent.getChildAt(childCount - 1);
    } else {
      while (childCount < (myTopMiddle ? 3 : 2)) {
        model.insertNodeInto(new DefaultMutableTreeNode(null), parent, 0);
        childCount++;
      }
      result = (DefaultMutableTreeNode) parent.getChildAt(myTopRoot ? 0 : childCount - 2);
    }
    result.setUserObject(component);
    return result;
  }

  public ObjectWrapper<?> getNode() {
    return (ObjectWrapper<?>) myPath.getLastPathComponent();
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    java.util.List<Object> lasts = Collections15.arrayList(myLastNode.length);
    for (boolean b : myLastNode)
      lasts.add(b);
    buffer.append(Arrays.asList(lasts)).append(" ");
    buffer.append(myPath);
    if (isLeaf())
      buffer.append(" leaf");
    if (isExpanded())
      buffer.append(" expanded");
    return buffer.toString();
  }

  enum NodeLocation {
    TOP_ROOT,
    MIDDLE_ROOT,
    GENERAL
  }
}
