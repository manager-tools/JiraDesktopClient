package com.almworks.util.components;

import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

// todo #1037
public interface ATreeNode<T> extends TreeNode, ObjectWrapper<T> {
  DataRole<ATreeNode> ATREE_NODE = DataRole.createRole(ATreeNode.class);
  DataRole<TreeNode> TREE_NODE = DataRole.createRole(TreeNode.class);

  ATreeNode<T> getChildAt(int childIndex);

  @Nullable
  ATreeNode<T> getParent();

  void add(ATreeNode<T> child);

  void addAllOrdered(Collection<ATreeNode<T>> children, Comparator<? super T> comparator);

  void addOrdered(ATreeNode<T> child, @Nullable Comparator<? super T> comparator);

  List<T> childObjectsToList();

  ATreeNode<T>[] childrenToArray();

  List<ATreeNode<T>> childrenToList();

  /**
   * Adds children to the list. Does not clear the list.
   *
   * @return destination
   */
  List<ATreeNode<T>> childrenToList(List<ATreeNode<T>> destination);

  void fireChanged();

  @Nullable
  ATreeNode<T> getNextSibling();

  TreePath getPathFromRoot();

  @Nullable
  ATreeNode<T> getPrevSibling();

  T getUserObject();

  void insert(ATreeNode<T> child, int index);

  boolean isAttachedToModel();

  ATreeNode<T> remove(int index);

  List<ATreeNode<T>> removeAll();

  int removeFromParent();

  void replaceRoot(DefaultTreeModel model);

  @Nullable
  ATreeNode<T> getChildOrNull(int childIndex);

  @NotNull
  ATreeNodeManager<T> getNodeManager();

  @NotNull
  ATreeNode<T> getRoot();

  Iterator<T> childObjectsIterator();
}
