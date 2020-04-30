package com.almworks.util.components;

import com.almworks.integers.IntList;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.Context;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/**
 * @author : Dyoma
 */
public class TreeModelBridge<T> extends AbstractATreeWritableNode<T> {
  public static final TreeModelBridge<?>[] EMPTY_ARRAY = new TreeModelBridge[0];
  public static final Condition<TreeNode> IS_ROOT = new Condition<TreeNode>() {
    public boolean isAccepted(TreeNode value) {
      return value.getParent() == null;
    }
  };
  public static final Convertor<TreeModelBridge<?>, TreePath> PATH_TO_ROOT =
    new Convertor<TreeModelBridge<?>, TreePath>() {
      public TreePath convert(TreeModelBridge<?> value) {
        assert value.isAttachedToModel();
        return value.getPathFromRoot();
      }
    };
  public static final Convertor<TreePath, TreeModelBridge<?>> LAST_NODE =
    new Convertor<TreePath, TreeModelBridge<?>>() {
      public TreeModelBridge<?> convert(TreePath value) {
        return (TreeModelBridge<?>) value.getLastPathComponent();
      }
    };

  @Nullable
  private List<TreeModelBridge<T>> myChildren;
  private DefaultTreeModel myModel = null;
  private FireEventSupport<TreeListener> myTreeEventSupport = null;
  @Nullable
  private T myUserObject = null;
  private TreeModelBridge<T> myParent = null;

  private static boolean ourGuardFromRecurrentToString = false;

  public TreeModelBridge(@Nullable T userObject) {
    myUserObject = userObject;
    // we don't need to notify ModelAware user object here because at construction time
    // the node is guaranteed not to be in any tree.
  }

  public static <T> TreeModelBridge<T> create(@Nullable T userObject) {
    return new TreeModelBridge<T>(userObject);
  }

  public TreeModelBridge<T> getChildAt(int childIndex) {
    Threads.assertAWTThread();
    List<TreeModelBridge<T>> children = myChildren;
    if (children == null)
      throw new IndexOutOfBoundsException("Size: 0, index: " + childIndex);
    assert childIndex >= 0 && childIndex < children.size() : childIndex + " " + children.size();
    return children.get(childIndex);
  }

  public TreeModelBridge<T> getParent() {
    return myParent;
  }

  protected void setParent(TreeModelBridge<T> parent) {
    myParent = parent;
  }

  public TreeModelBridge<T>[] childrenToArray() {
    Threads.assertAWTThread();
    List<TreeModelBridge<T>> children = myChildren;
    return children != null ? children.toArray(new TreeModelBridge[children.size()]) : (TreeModelBridge<T>[]) EMPTY_ARRAY;
  }

  public TreePath getPathFromRoot() {
    return UIUtil.getPathToRoot(this);
  }

  public void replaceRoot(DefaultTreeModel model) {
    // todo #1037
    Object oldRoot = model.getRoot();
    if (oldRoot == this)
      return;
    assert getParent() == null;
    if (oldRoot != null)
      ((TreeModelBridge<?>) oldRoot).onRemoveFromModel();
    model.setRoot(this);

    onInsertToModel(model);
  }

  @NotNull
  public ATreeNodeManager<T> getNodeManager() {
    return PhysicalNodesManager.INSTANCE;
  }

  public List<ATreeNode<T>> childrenToList() {
    Threads.assertAWTThread();
    return myChildren == null ? Collections15.<ATreeNode<T>>arrayList(0) :
      Collections15.<ATreeNode<T>>arrayList(myChildren);
  }

  public List<ATreeNode<T>> childrenToList(List<ATreeNode<T>> destination) {
    Threads.assertAWTThread();
    if (myChildren != null)
      destination.addAll(myChildren);
    return destination;
  }

  public void setUserObject(T object) {
    T oldObject = getUserObject();
    if ((oldObject instanceof ModelAware) && isAttachedToModel())
      ((ModelAware) oldObject).onRemoveFromModel();
    myUserObject = object;
    if ((object instanceof ModelAware) && isAttachedToModel())
      ((ModelAware) object).onInsertToModel();
    if (!Util.equals(oldObject, object))
      fireChanged();
  }

  @Nullable
  public T getUserObject() {
    return myUserObject;
  }

  public int getChildCount() {
    Threads.assertAWTThread();
    return myChildren != null ? myChildren.size() : 0;
  }

  public int getIndex(TreeNode node) {
    Threads.assertAWTThread();
    //noinspection SuspiciousMethodCalls
    return myChildren != null ? myChildren.indexOf(node) : -1;
  }

  public boolean getAllowsChildren() {
    return true;
  }

  public boolean isLeaf() {
    return getChildCount() == 0;
  }

  public Enumeration<TreeModelBridge<T>> children() {
    Threads.assertAWTThread();
    return myChildren != null ? Collections.enumeration(myChildren) :
      Collections15.<TreeModelBridge<T>>emptyEnumeration();
  }

  public DefaultTreeModel becomeRoot() {
    assert getParent() == null;
    onInsertToModel(new DefaultTreeModel(this) {
      public void nodeChanged(TreeNode node) {
        assert Context.isAWT();
        super.nodeChanged(node);
      }

      public void nodeStructureChanged(TreeNode node) {
        assert Context.isAWT();
        super.nodeStructureChanged(node);
      }

      public void nodesChanged(TreeNode node, int[] childIndices) {
        assert Context.isAWT();
        super.nodesChanged(node, childIndices);
      }

      public void nodesWereInserted(TreeNode node, int[] childIndices) {
        assert Context.isAWT();
        super.nodesWereInserted(node, childIndices);
      }
    });
    return myModel;
  }

  public void fireChanged() {
    if (myModel != null)
      myModel.nodeChanged(this);
    fireChanged_DontNotifyModel();
  }

  public void fireChildrenChanged(IntList indexesList) {
    if (indexesList == null || indexesList.isEmpty()) return;
    int[] indexes = indexesList.toNativeArray();
    if (myModel != null) myModel.nodesChanged(this, indexes);
  }

  @Deprecated
  public void fireChanged_DontNotifyModel() {
    fireTreeChanged(TreeEvent.nodeChanged(this));
  }

  public EventSource<TreeListener> getTreeEventSource() {
    if (myTreeEventSupport == null)
      myTreeEventSupport = FireEventSupport.create(TreeListener.class);
    return myTreeEventSupport;
  }

  public void insert(ATreeNode<T> child, int index) {
    Threads.assertAWTThread();
    assert child != this;
    assert child.getParent() == null;
    assert child instanceof TreeModelBridge;
    TreeModelBridge<T> typed = ((TreeModelBridge<T>) child);
    typed.setParent(this);
    if (myChildren == null)
      myChildren = Collections15.arrayList(4);
    assert myChildren != null;
    myChildren.add(index, typed);
    if (myModel != null) {
      myModel.nodesWereInserted(this, new int[] {index});
      typed.onInsertToModel(myModel);
    }
    onChildrenChanged();
    fireTreeChanged(TreeEvent.nodeInserted(this, typed));
  }

  private void onInsertToModel(DefaultTreeModel model) {
    assert myModel == null;
    assert model != null;
    myModel = model;
    T object = getUserObject();
    List<TreeModelBridge<T>> ch = myChildren;
    TreeModelBridge<?>[] children = ch == null ? null : ch.toArray(new TreeModelBridge[ch.size()]);
    if (object instanceof ModelAware) {
      ((ModelAware) object).onInsertToModel();
    }
    if (children != null) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < children.length; i++) {
        TreeModelBridge<?> child = children[i];
        if (child.getParent() == this)
          child.onInsertToModel(model);
      }
    }
  }

  public boolean isAttachedToModel() {
    return myModel != null;
  }

  /**
   * Pulls itself out of tree. The reciever should not be a tree model root. All children of this node are kept in tree.
   * They are inserted into reciever's parent at recievers index.
   */
  public void pullOut() {
    TreeModelBridge<T> parent = getParent();
    assert parent != null : this;
    int index = removeFromParent();
    moveChildren(parent, index);
  }

  public TreeModelBridge<T> remove(int index) {
    Threads.assertAWTThread();
    List<TreeModelBridge<T>> children = myChildren;
    if (children == null)
      throw new IndexOutOfBoundsException("Size: 0, index: " + index);
    TreeModelBridge<T> removed = children.remove(index);
    if (children.size() == 0)
      myChildren = null;
    if (myModel != null)
      removed.onRemoveFromModel();
    removed.setParent(null);
    if (myModel != null)
      myModel.nodesWereRemoved(this, new int[] {index}, new Object[] {removed});
    onChildrenChanged();
    fireTreeChanged(TreeEvent.nodeRemoved(this, removed));
    return removed;
  }

  public List<ATreeNode<T>> removeAll() {
    Threads.assertAWTThread();
    if (myChildren == null)
      return Collections15.emptyList();
    List<TreeModelBridge<T>> children = myChildren;
    List<ATreeNode<T>> removed = Collections15.<ATreeNode<T>>arrayList(children);
    myChildren = null;
    int[] indecies = new int[children.size()];
    for (int i = 0; i < children.size(); i++) {
      TreeModelBridge<T> child = children.get(i);
      if (myModel != null)
        child.onRemoveFromModel();
      child.setParent(null);
      indecies[i] = i;
    }
    if (myModel != null)
      myModel.nodesWereRemoved(this, indecies, children.toArray());
    fireTreeChanged(TreeEvent.nodesRemoved(this, children));
    return removed;
  }

  private void onRemoveFromModel() {
    if (myModel == null) LogHelper.error("Null model");

    for (int i = 0; i < getChildCount(); i++)
      getChildAt(i).onRemoveFromModel();
    myModel = null;

    T object = getUserObject();
    if (object instanceof ModelAware)
      ((ModelAware) object).onRemoveFromModel();
  }

  private void onChildrenChanged() {
    T object = getUserObject();
    if (object instanceof ModelAware)
      ((ModelAware) object).onChildrenChanged();
  }

  private void fireTreeChanged(TreeEvent<T> event) {
    fireTreeChanged(this, event);
    TreeModelBridge<T> node = this;
    while (node != null) {
      fireTreeChanged(node, event);
      node = node.getParent();
      if (node == this) {
        assert false : "cycle with " + this;
        break;
      }
    }
  }

  private static <T> void fireTreeChanged(TreeModelBridge<T> node, TreeEvent<T> event) {
    if (node.myTreeEventSupport != null)
      node.myTreeEventSupport.getDispatcher().onTreeEvent(event);
  }

  /**
   * Replaces this node with newNode. newNode shouldn't have childen. All children of this node becomes children of newNode
   * This node should not be the root of tree model.
   */
  public void replaceWith(TreeModelBridge<T> newNode) {
    //noinspection ConstantConditions
    assert newNode instanceof TreeModelBridge<?>;
    assert newNode.getParent() == null : newNode + " " + newNode.getParent();
    assert newNode.getChildCount() == 0 : newNode.childrenToList();
    TreeModelBridge<T> parent = getParent();
    assert parent != null : this;
    parent.insert(newNode, removeFromParent());
    moveChildren(newNode, 0);
  }

  /**
   * Move all own children to newParent. First child is inserted at index. The order of children is presevered.
   */
  private void moveChildren(TreeModelBridge<T> newParent, int index) {
    for (TreeModelBridge<T> child : childrenToArray()) {
      child.removeFromParent();
      newParent.insert(child, index);
      index++;
    }
  }

  public String toString() {
    if (ourGuardFromRecurrentToString) {
      return "TMB[!]";
    } else {
      ourGuardFromRecurrentToString = true;
      try {
        return "TMB[" + myUserObject + "]";
      } finally {
        ourGuardFromRecurrentToString = false;
      }
    }
  }

  public void updateOrder(@NotNull final Comparator<? super T> comparator) {
    List<ATreeNode<T>> children = childrenToList();
    Collections.sort(children, new Comparator<ATreeNode<T>>() {
      public int compare(ATreeNode<T> o1, ATreeNode<T> o2) {
        return comparator.compare(o1.getUserObject(), o2.getUserObject());
      }
    });
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      assert i <= getChildCount();
      ATreeNode<T> child = children.get(i);
      if (i == getChildCount()) {
        insert(child, getChildCount());
        continue;
      }
      TreeModelBridge<? extends T> oldChild = getChildAt(i);
      if (oldChild == child)
        continue;
      oldChild.removeFromParent();
      if (child.getParent() != null)
        child.removeFromParent();
      insert(child, i);
    }
  }

  /**
   * Wedges in <code>newNode</code>. The <code>newNode</code> becomes direct child of <code>this</code> and direct
   * parent of <code>child</code>. <code>newNode replaces <code>child at it's index.
   *
   * @param newNode new node to be inserted. Shouldn't have parent.
   * @param child   direct child of <code>this</code>
   */
  public void wedgeIn(TreeModelBridge<T> newNode, TreeModelBridge<T> child) {
    assert child.getParent() == this : child.getParent();
    assert newNode.getParent() == null;
    int index = child.removeFromParent();
    newNode.add(child);
    insert(newNode, index);
  }

  /**
   * Replaces itself with <code>newParent</code> and add itself to <code>newParent</code>.
   */
  public void wedgeInParent(TreeModelBridge<T> newParent) {
    assert newParent.getParent() == null : newParent.getParent();
    TreeModelBridge<T> parent = getParent();
    assert parent != null : this;
    parent.insert(newParent, removeFromParent());
    newParent.add(this);
  }

  @NotNull
  public TreeModelBridge<T> getRoot() {
    return (TreeModelBridge<T>) super.getRoot();
  }

  @Nullable
  public DefaultTreeModel getTreeModel() {
    return myModel;
  }

  public List<TreeModelBridge<T>> getAncestors() {
    List<TreeModelBridge<T>> list = Collections15.arrayList();
    TreeModelBridge<T> ancestor = getParent();
    while (ancestor != null) {
      list.add(ancestor);
      ancestor = ancestor.getParent();
    }
    return list;
  }

  public void moveTo(ATreeNode<?> newParent, @Nullable Comparator<? super T> comparator) {
    TreeModelBridge<T> parent = getParent();
    if (parent == null) {
      assert false : this;
      return;
    }
    if (parent == newParent)
      return;
    assert !isAncestorOf(newParent) : this + " " + newParent;
    removeFromParent();
    ((ATreeNode<T>) newParent).addOrdered(this, comparator);
  }

  public boolean isAncestorOf(ATreeNode<?> node) {
    while (node != null) {
      if (this == node)
        return true;
      node = node.getParent();
    }
    return false;
  }

  public int[] getChildIndecies(List<? extends TreeModelBridge<?>> nodes) {
    int[] result = new int[nodes.size()];
    int resultPos = 0;
    List<TreeModelBridge<T>> children = myChildren;
    if (children != null) {
      for (TreeModelBridge node : nodes) {
        int index = children.indexOf(node);
        if (index < 0)
          continue;
        result[resultPos] = index;
        resultPos++;
      }
    }
    if (resultPos != result.length) {
      assert resultPos < result.length;
      int[] tmp = new int[resultPos];
      System.arraycopy(result, 0, tmp, 0, tmp.length);
      result = tmp;
    }
    return result;
  }

  private static final class PhysicalNodesManager<T> implements ATreeNodeManager<T> {
    @SuppressWarnings({"RawUseOfParameterizedType"})
    public static final PhysicalNodesManager INSTANCE = new PhysicalNodesManager();

    public ATreeNode<T> findCorresponding(ATreeNode<T> subject) {
      return subject;
    }
  }
}
