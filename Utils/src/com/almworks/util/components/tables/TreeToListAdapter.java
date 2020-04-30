package com.almworks.util.components.tables;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ConvertingListDecorator;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.CollectionSortPolicy;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.ui.TreeUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author dyoma
 */
public class TreeToListAdapter<T> {
  private final OrderListModel<TreePath> myPathModel;
  private final ConvertingListDecorator<TreePath, T> myListModel;
  private final TreeModel myModel;
  private final FireEventSupport<TreeExpansionListener> myListeners = FireEventSupport.create(TreeExpansionListener.class);
  @Nullable
  private Comparator<? super T> myComparator = null;
  @Nullable
  private Comparator<TreePath> myPathOrder;
  private boolean myRootVisible = true;

  private CollectionSortPolicy mySortPolicy = CollectionSortPolicy.DEFAULT;

  private TreeToListAdapter(TreeModel model) {
    myModel = model;
    Object root = model.getRoot();
    TreePath path = new TreePath(root);
    myPathModel = OrderListModel.create(path);
    myListModel = ConvertingListDecorator.create(myPathModel, TreeUtil.<T>getLastPathUserObject());
    expand(path);
  }

  public TreeToListAdapter() {
    throw new UnsupportedOperationException();
  }

  public Detach addExpansionListener(TreeExpansionListener listener) {
    return myListeners.addStraightListener(listener);
  }

  public void expand(TreePath path) {
    Object node = path.getLastPathComponent();
    int childCount = myModel.getChildCount(node);
    if (childCount == 0)
      return;
    int index;
    if (!myRootVisible && path.getPathCount() == 1) {
      if (!Util.equals(myModel.getRoot(), node)) {
        assert false : node + " " + myModel.getRoot();
        return;
      }
      if (myPathModel.getSize() <= 0)
        assert false;
      return;
    } else {
      index = getPathIndex(path);
      if (index < 0) {
        assert false : path;
        return;
      }
      if (isExpanded(index))
        return;
      path = getPathAt(index);
    }
    TreePath[] children = new TreePath[childCount];
    for (int i = 0; i < childCount; i++)
      children[i] = path.pathByAddingChild(myModel.getChild(node, i));
    if (myPathOrder != null)
      Arrays.sort(children, myPathOrder);
    myPathModel.insertAll(index + 1, children);
    myPathModel.updateAt(index);
    myListeners.getDispatcher().treeExpanded(new TreeExpansionEvent(this, path));
  }

  public void expandAll() {
    TreeExpansionListener dispatcher = myListeners.getDispatcher();
    for (int i = 0; i < myPathModel.getSize(); i++) {
      TreePath path = myPathModel.getAt(i);
      TreePath nextPath = i < myPathModel.getSize() - 1 ? myPathModel.getAt(i + 1) : null;
      if (nextPath != null && nextPath.getParentPath() == path) {
        // already expanded: next row is a child
        continue;
      }
      assert nextPath == null || !Util.equals(nextPath.getParentPath(), path);
      Object node = path.getLastPathComponent();
      int childCount = myModel.getChildCount(node);
      if (childCount == 0) {
        // no children
        continue;
      }
      TreePath[] children = new TreePath[childCount];
      for (int ci = 0; ci < childCount; ci++)
        children[ci] = path.pathByAddingChild(myModel.getChild(node, ci));
      if (myPathOrder != null)
        Arrays.sort(children, myPathOrder);
      myPathModel.insertAll(i + 1, children);
      myPathModel.updateAt(i);
      dispatcher.treeExpanded(new TreeExpansionEvent(this, path));
    }
  }

  public void collapse(TreePath path) {
    int index = getPathIndex(path);
    if (index == -1) {
      assert !myRootVisible : path;
      assert path.getPathCount() == 1 : path;
      assert Util.equals(path.getLastPathComponent(), myModel.getRoot()) : path;
      return;
    }
    int lastDescendant = getLastDescendant(index);
    if (lastDescendant == -1)
      return;
    myPathModel.removeRange(index + 1, lastDescendant);
    myPathModel.updateAt(index);
    myListeners.getDispatcher().treeCollapsed(new TreeExpansionEvent(this, path));
  }

  public AListModel<T> getListModel() {
    return myListModel;
  }

  @Nullable
  public Comparator<? super T> getOrder() {
    return myComparator;
  }

  public void sort(@Nullable Comparator<? super T> comparator) {
    sort(comparator, CollectionSortPolicy.DEFAULT);
  }
  
  public void sort(@Nullable Comparator<? super T> comparator, CollectionSortPolicy sortPolicy) {
    myComparator = comparator;
    if (comparator == null) {
      myPathOrder = null;
    } else {
      myPathOrder = TreeUtil.treeUserObjectOrder(comparator);
      mySortPolicy = sortPolicy;
      myPathModel.sort(myPathOrder, sortPolicy);
    }
  }

  @Nullable
  public Comparator<? super T> getComparator() {
    return myComparator;
  }

  @NotNull
  public static <T> TreeToListAdapter<T> create(Lifespan lifespan, TreeModel model) {
    TreeToListAdapter<T> adapter = new TreeToListAdapter<T>(model);
    adapter.new MyDetachingTreeModelListener(model).attach(lifespan);
    return adapter;
  }

  public int getRowCount() {
    return myPathModel.getSize();
  }

  public TreePath getPathAt(int index) {
    return myPathModel.getAt(index);
  }

  @Nullable
  public T getFirstChild(int index) {
    Object node = getPathAt(index).getLastPathComponent();
    return (T) (myModel.getChildCount(node) == 0 ? null : myModel.getChild(node, 0));
  }

  /**
   * @return true if row at specified index isn't leaf and currently is expanded. I.e. the next row is its child.
   */
  public boolean isExpanded(int index) {
    if (index == getRowCount() - 1)
      return false;
    TreePath path = getPathAt(index);
    TreePath nextPath = getPathAt(index + 1);
    return path == nextPath.getParentPath();
  }

  public boolean isExpanded(TreePath path) {
    if (!isRootVisible() && path.getPathCount() == 1)
      return true;
    return isExpanded(getPathIndex(path));
  }

  public int getParentIndex(int index) {
    TreePath parentPath = getPathAt(index).getParentPath();
    if (parentPath == null)
      return -1;
    for (int i = index - 1; i >= 0; i--)
      if (parentPath == getPathAt(i))
        return i;
    return -1;
  }

  public boolean isSameParent(int index1, int index2) {
    TreePath path1 = getPathAt(index1);
    TreePath path2 = getPathAt(index2);
    return path1.getPath() == path2.getPath();
  }

  /**
   * @return Last descendant index of specified node or -1 if node has no expanded children
   */
  public int getLastDescendant(int index) {
    if (index >= getRowCount() - 1)
      return -1;
    if (index == -1 && !myRootVisible)
      return getRowCount() - 1;
    TreePath ancestor = getPathAt(index);
    TreePath parent = ancestor;
    int count = ancestor.getPathCount();
    int i = index + 1;
    for (; i < getRowCount(); i++) {
      TreePath path = getPathAt(i);
      if (path.getPathCount() <= count)
        break;
      if (!isDecendant(ancestor, path)) {
        assert false : "Ancestor:" + ancestor + " descendant:" + path;
        break;
      }
    }
    return i == index + 1 ? -1 : i - 1;
  }

  private boolean isDecendant(TreePath ancestor, TreePath decendant) {
    int ancestorCount = ancestor.getPathCount();
    int decendantCount = decendant.getPathCount();
    if (ancestorCount >= decendantCount)
      return false;
    for (int i = 0; i < decendantCount - ancestorCount; i++)
      decendant = decendant.getParentPath();
    return Util.equals(ancestor, decendant);
  }

  public boolean isLastChild(int index) {
    if (index < 0)
      return true;
    int pathCount = getPathAt(index).getPathCount();
    for (int i = index + 1; i < getRowCount(); i++) {
      int count = getPathAt(i).getPathCount();
      if (count == pathCount)
        return false;
      if (count < pathCount)
        break;
    }
    return true;
  }

  public int getPathIndex(@NotNull TreePath path) {
    return myPathModel.indexOf(path);
  }

  public void setRootVisible(boolean visible) {
    if (myRootVisible == visible)
      return;
    myRootVisible = visible;
    if (visible) {
      myPathModel.insert(0, getRootPath());
    } else {
      assert myPathModel.getSize() > 0;
      assert myPathModel.getAt(0).getPathCount() == 1;
      myPathModel.removeAt(0);
      myPathModel.updateAll();
    }
  }

  public boolean isRootVisible() {
    return myRootVisible;
  }

  private TreePath getRootPath() {
    if (myPathModel.getSize() == 0)
      return new TreePath(myModel.getRoot());
    else {
      assert myPathModel.getAt(0).getPathCount() == 2;
      return myPathModel.getAt(0).getParentPath();
    }
  }

  private class MyDetachingTreeModelListener extends Detach implements TreeModelListener {
    private TreeModel myModel;
    private boolean myStopEvents;

    private MyDetachingTreeModelListener(TreeModel model) {
      myModel = model;
    }

    private void attach(Lifespan lifespan) {
      TreeModel model = myModel;
      if (lifespan.isEnded() || model == null) {
        myModel = null;
        return;
      }
      model.addTreeModelListener(this);
      lifespan.add(this);
    }

    protected void doDetach() throws Exception {
      TreeModel model = myModel;
      if (model != null) {
        model.removeTreeModelListener(this);
        myModel = null;
      }
    }

    public void preDetach() {
      myStopEvents = true;
    }

    public void treeNodesChanged(TreeModelEvent e) {
      if (myStopEvents)
        return;
      TreePath path = getModelPath(e.getPath(), true);
      if (path == null)
        return;
      int index = getPathIndex(path);
      int[] childIndices = e.getChildIndices();
      if (childIndices != null && childIndices.length == 1 && myPathOrder == null) {
        Object child = myModel.getChild(path.getLastPathComponent(), childIndices[0]);
        myPathModel.updateElement(path.pathByAddingChild(child));
      } else {
        int firstChild = index + 1;
        int lastChild = getLastDescendant(index);
        if (lastChild < firstChild)
          return;
        if (myPathOrder != null)
          myPathModel.sort(firstChild, lastChild, myPathOrder, mySortPolicy);
        myPathModel.updateRange(firstChild, lastChild); // todo performance!
      }
    }

    public void treeNodesInserted(TreeModelEvent e) {
      if (myStopEvents)
        return;
      TreePath path = getModelPath(e.getPath(), false);
      if (path == null)
        return;
      Object parentNode = path.getLastPathComponent();
      int[] indices = e.getChildIndices();
      if (indices.length < myModel.getChildCount(parentNode) && !isExpanded(path))
        return;
      if (myPathOrder == null) {
        int insertionIndex = getPathIndex(path);
        assert insertionIndex >= 0 || (path.getPathCount() == 1 && !isRootVisible()) : path;
        insertionIndex++;
        int childIndex = 0;
        for (int i = 0; i < indices.length; i++) {
          int index = indices[i];
          assert index >= childIndex : index + " " + childIndex;
          while (index > childIndex) {
            int last = getLastDescendant(insertionIndex);
            insertionIndex = last == -1 ? insertionIndex + 1 : last + 1;
            assert insertionIndex <= getRowCount() : insertionIndex + " " + getRowCount();
            childIndex++;
          }
          Object child = myModel.getChild(parentNode, index);
          TreePath newChild = path.pathByAddingChild(child);
          myPathModel.insert(insertionIndex, newChild);
          childIndex++;
          insertionIndex++;
        }
      } else {
        for (int i : indices) {
          Object child = myModel.getChild(parentNode, i);
          TreePath newChild = path.pathByAddingChild(child);
          // myPathOrder != null because it can be set to null in AWT thread only
          //noinspection ConstantConditions
          myPathModel.addElement(newChild, myPathOrder);
        }
      }
    }

    public void treeNodesRemoved(TreeModelEvent e) {
      if (myStopEvents)
        return;
      TreePath path = getModelPath(e.getPath(), true);
      if (path == null)
        return;
      Object[] children = e.getChildren();
      int[] toRemove = new int[children.length];
      for (int i = 0; i < children.length; i++) {
        Object removedChild = children[i];
        toRemove[i] = getPathIndex(path.pathByAddingChild(removedChild));
        assert toRemove[i] >= 0 : toRemove[i];
      }
      Arrays.sort(toRemove);
      for (int i = toRemove.length - 1; i >= 0; i--) {
        int index = toRemove[i];
        if (index < 0) continue;
        int lastIndex = getLastDescendant(index);
        lastIndex = lastIndex != -1 ? lastIndex : index;
        myPathModel.removeRange(index, lastIndex);
      }
    }

    public void treeStructureChanged(TreeModelEvent e) {
      if (myStopEvents)
        return;
      TreePath path = getModelPath(e.getPath(), true);
      if (path == null)
        return;
      collapse(path);
      expand(path);
    }

    @Nullable
    private TreePath getModelPath(Object[] components, boolean expanded) {
      if (components.length == 1 && !isRootVisible())
        return getRootPath();
      TreePath path = new TreePath(components);
      int index = getPathIndex(path);
      if (index < 0 || (expanded && !isExpanded(index)))
        return null;
      return getPathAt(index);
    }
  }
}
