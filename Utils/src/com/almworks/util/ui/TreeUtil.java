package com.almworks.util.ui;

import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.ObjectWrapper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author : Dyoma
 */
public class TreeUtil {
  public static final Convertor<ATreeNode, ATreeNode> PARENT = new Convertor<ATreeNode, ATreeNode>() {
    public ATreeNode convert(ATreeNode value) {
      //noinspection ConstantConditions
      return value.getParent();
    }
  };

  public static final Convertor<TreePath, Object> LAST_PATH_COMPONENT = new Convertor<TreePath, Object>() {
    public Object convert(TreePath value) {
      return value.getLastPathComponent();
    }
  };

  private static final Convertor<TreePath, Object> LAST_PATH_USER_OBJECT = new Convertor<TreePath, Object>() {
    public Object convert(TreePath value) {
      Object obj = value.getLastPathComponent();
      return obj instanceof ObjectWrapper<?> ? ((ObjectWrapper<Object>) obj).getUserObject() : obj;
    }
  };
  private static final TreePath[] EMPTY_TREE_PATHS = new TreePath[0];
  public static final Convertor<TreeNode,TreeNode> TREENODE_PARENT_FUNCTION = new Convertor<TreeNode, TreeNode>() {
   public TreeNode convert(TreeNode value) {
     return value.getParent();
   }
  };

  /**
   * @return -1 node1 is descendant of node2.<br> 1 node2 is descendant of node1<br> 0 none of given nodes is an ancestor of another
   */
  public static int treeOrder(TreeNode node1, TreeNode node2) {
    if (isAncestor(node2, node1))
      return -1;
    else if (isAncestor(node1, node2))
      return 1;
    return 0;
  }

  public static boolean isAncestor(TreeNode ancestor, TreeNode descendant) {
    TreeNode parent = descendant;
    while (parent != null) {
      if (parent == ancestor)
        return true;
      parent = parent.getParent();
    }
    return false;
  }

  public static boolean isDescendantOfAny(TreeNode descendant, List<? extends TreeNode> nodes) {
    for (int i = 0; i < nodes.size(); i++) {
      TreeNode node = nodes.get(i);
      if (isAncestor(node, descendant))
        return true;
    }
    return false;
  }

  public static <T extends TreeNode> Set<T> excludeDescendants(Collection<? extends T> nodes) {
    // old algorithm had complexity K*K*logN, where K=nodes.size(), N=tree size
    // rewritten with K*logN.
    return TreeUtil.<T>excludeDescendants(nodes, (Convertor)TREENODE_PARENT_FUNCTION);
  }

  public static <T> Set<T> excludeDescendants(Collection<? extends T> nodes, Convertor<? super T, ? extends T> parentFunction) {
    Set<T> result = Collections15.linkedHashSet();
    Set<T> resultParents = Collections15.hashSet();
    for (T node : nodes) {
      if (resultParents.remove(node)) {
        for (Iterator<T> ii = result.iterator(); ii.hasNext();) {
          T currentResultNode = ii.next();
          if (excludeDescendants_removeParentedBy(currentResultNode, node, resultParents, parentFunction)) {
            ii.remove();
          }
        }
        result.add(node);
      } else if (!result.contains(node)) {
        if (excludeDescendants_addParentNotParentedByAny(node, result, resultParents, parentFunction)) {
          result.add(node);
        }
      }
    }
    return result;
  }

  private static <T> boolean excludeDescendants_addParentNotParentedByAny(T node, Set<T> resultSet, Set<T> resultParents,
    Convertor<? super T, ? extends T> parentFunction)
  {
    T p = parentFunction.convert(node);
    boolean result;
    if (p == null)
      result = true;
    else if (resultSet.contains(p))
      result = false;
    else
      result = excludeDescendants_addParentNotParentedByAny(p, resultSet, resultParents, parentFunction);
    if (result) {
      resultParents.add(p);
    }
    return result;
  }

  private static <T> boolean excludeDescendants_removeParentedBy(T suspectChild, T suspectParent, Set<T> resultParents,
    Convertor<? super T, ? extends T> parentFunction)
  {
    T p = parentFunction.convert(suspectChild);
    boolean result;
    if (p == null)
      result = false;
    else if (p.equals(suspectParent))
      result = true;
    else
      result = excludeDescendants_removeParentedBy(p, suspectParent, resultParents, parentFunction);
    if (result) {
      resultParents.remove(p);
    }
    return result;
  }

  @Nullable
  public static ATreeNode commonParent(List<? extends ATreeNode> nodes) {
    assert Condition.notNull().areAll(nodes) : nodes;
    // todo robustness or error hiding?
    nodes = Condition.<ATreeNode>notNull().maybeFilterList(nodes);
    return commonAncestor(nodes, PARENT);
  }

  /**
   * Finds deepest common ancestor of tree nodes. Tree structure is specified via "get parent" operation. tree may not has null nodes, except topmost root -
   * wich is always common ancestor.
   * @param nodes  source nodes to find deepest common ancestor.
   * @param getParent "get parent" operation
   * @return common ancestor (member of nodes or one returned by getParent). null means no common ancestor is detected so top most root (null) is returned.
   */
  public static <T> T commonAncestor (Collection<? extends T> nodes, Convertor<? super T, ? extends T> getParent) {
    boolean anyNullNodes = Condition.<T>isNull().hasAny(nodes);
    if (anyNullNodes) return null;
    Set<? extends T> result = excludeDescendants(nodes, getParent);
    while (result.size() > 1) {
      result = getParent.collectSet(result);
      if (!Condition.notNull().areAll(result)) return null;
      result = excludeDescendants(result, getParent);
    }
    return result.size() == 0 ? null : result.iterator().next();
  }

  public static ATreeNode getRoot(ATreeNode<?> node) {
    ATreeNode<?> parent;
    while ((parent = node.getParent()) != null)
      node = parent;
    return node;
  }

  @Nullable
  public static <T> T detectUserObject(Collection<? extends ObjectWrapper<? extends T>> wrappers,
    Condition<? super T> condition)
  {
    for (ObjectWrapper<? extends T> wrapper : wrappers) {
      T userObject = wrapper.getUserObject();
      if (condition.isAccepted(userObject))
        return userObject;
    }
    return null;
  }

  public static <T> List<T> selectUsetObjects(List<? extends ObjectWrapper<? extends T>> wrappers,
    Condition<T> condition)
  {
    List<T> result = Collections15.arrayList();
    for (ObjectWrapper<? extends T> wrapper : wrappers) {
      T userObject = wrapper.getUserObject();
      if (condition.isAccepted(userObject))
        result.add(userObject);
    }
    return result;
  }

  @Nullable
  public static TreePath getNextVisiblePath(@NotNull TreePath path, @NotNull JTree tree) {
    @Nullable TreePath expandedPath = path;
    Object visibleNode = expandedPath.getLastPathComponent();
    while (expandedPath != null && !tree.isExpanded(expandedPath)) {
      visibleNode = expandedPath.getLastPathComponent();
      expandedPath = expandedPath.getParentPath();
    }
    TreeModel model = tree.getModel();
    while (expandedPath != null) {
      Object parent = expandedPath.getLastPathComponent();
      int index = model.getIndexOfChild(parent, visibleNode);
      if (index < model.getChildCount(parent) - 1)
        return expandedPath.pathByAddingChild(model.getChild(parent, index + 1));
      expandedPath = expandedPath.getParentPath();
      visibleNode = parent;
    }
    return null;
  }

  public static List getVisibleChildren(@NotNull TreePath path, @NotNull JTree tree) {
    return LAST_PATH_COMPONENT.collectList(getVisibleChildrenPaths(path, tree));
  }

  public static List<TreePath> getVisibleChildrenPaths(TreePath path, JTree tree) {
    List<TreePath> list = Collections15.arrayList();
    addVisibleChildrenPaths(path, tree, list);
    return list;
  }

  public static void addVisibleChildrenPaths(TreePath path, JTree tree, List<TreePath> list) {
    if (tree.isCollapsed(path))
      return;
    Object ancestor = path.getLastPathComponent();
    TreeModel model = tree.getModel();
    int childCount = model.getChildCount(ancestor);
    for (int i = 0; i < childCount; i++) {
      Object child = model.getChild(ancestor, i);
      TreePath childPath = path.pathByAddingChild(child);
      list.add(childPath);
      addVisibleChildrenPaths(childPath, tree, list);
    }
  }

  public static <T> Convertor<TreePath, T> getLastPathUserObject() {
    return (Convertor<TreePath, T>) LAST_PATH_USER_OBJECT;
  }

  /**
   * Trancates not common tails of given paths except first not common component.
   *
   * @return {@link TreePath}s with longest same parentPath sequence. Paths are return in corresponding order.<br>
   *         If source path is longer then common path the last component is the first not common component of the source path.
   *         <br><br><i>Expample:</i><br>
   *         <b>Argument:</b> [(1,2,a,b) (1,2) (1,2,x) (1,2,Q,W,E)]<br>
   *         <b>Result:</b> [(1,2,a) (1,2) (1,2,x) (1,2,Q)]
   */
  public static TreePath[] commonAncestorPaths(TreePath[] paths) {
    if (paths.length == 0)
      return EMPTY_TREE_PATHS;
    List<Object> commonComponents = Collections15.arrayList();
    int element = 0;
    boolean common;
    do {
      common = true;
      Object commonComponent = null;
      for (int i = 0; i < paths.length; i++) {
        TreePath path = paths[i];
        if (element >= path.getPathCount()) {
          common = false;
          break;
        }
        Object pathComponent = path.getPathComponent(element);
        if (i == 0)
          commonComponent = pathComponent;
        else
          //noinspection ConstantConditions
          common &= Util.equals(commonComponent, pathComponent);
        if (!common)
          break;
      }
      if (common)
        commonComponents.add(commonComponent);
      element++;
    } while (common);
    element--;
    @Nullable TreePath commonPath = !commonComponents.isEmpty() ? new TreePath(commonComponents.toArray()) : null;
    TreePath[] result = new TreePath[paths.length];
    for (int i = 0; i < paths.length; i++) {
      TreePath path = paths[i];
      if (path.getPathCount() > element) {
        Object notCommon = path.getPathComponent(element);
        path = commonPath != null ? commonPath.pathByAddingChild(notCommon) : new TreePath(notCommon);
      }
      result[i] = path;
    }
    return result;
  }

  /**
   * @param siblingComparator compares siblings in tree
   * @return comparator to sort tree structure as list in such way that parents are always less then
   *         children (preserving tree structure).
   */
  @NotNull
  public static Comparator<TreePath> treeOrder(@NotNull final Comparator<TreePath> siblingComparator) {
    return new Comparator<TreePath>() {
      public int compare(TreePath path1, TreePath path2) {
        if (path1.isDescendant(path2))
          return -1;
        if (path2.isDescendant(path1))
          return 1;
        if (Util.equals(path1.getParentPath(), path2.getParentPath()))
          return siblingComparator.compare(path1, path2);
        TreePath[] commonPaths = TreeUtil.commonAncestorPaths(new TreePath[] {path1, path2});
        TreePath parent1 = commonPaths[0];
        TreePath parent2 = commonPaths[1];
        if (parent1.getPathCount() != parent2.getPathCount())
          return Containers.compareInts(parent1.getPathCount(), parent2.getPathCount());
        assert !parent1.equals(parent2);
        return siblingComparator.compare(parent1, parent2);
      }
    };
  }

  @NotNull
  public static <T> Comparator<TreePath> treeUserObjectOrder(@NotNull Comparator<T> siblingComparator) {
    return treeOrder(TreeUtil.<T>getLastPathUserObject().comparator(siblingComparator));
  }

  public static TreePath pathFromRoot(TreeNode node) {
    List<TreeNode> path = Collections15.arrayList();
    for (; node != null; node = node.getParent())
      path.add(node);
    Collections.reverse(path);
    return new TreePath(path.toArray());
  }
}