package com.almworks.util.components.tables;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public class TreePathToTreeModel implements TreeModel {
  private static final TypedKey<List<TreeModelEvent>> EVENTS = TypedKey.create("events");
  private final AListModel<TreePath> myListModel;
  private final Set<Object> myNotInList = Collections15.hashSet();
  private final FireEventSupport<TreeModelListener> myListeners = FireEventSupport.create(TreeModelListener.class);
  private final TreeModel myOriginalTreeModel;

  public TreePathToTreeModel(AListModel<TreePath> listModel, TreeModel originalTreeModel) {
    myListModel = listModel;
    myOriginalTreeModel = originalTreeModel;
    ListListener listener = new ListListener();
    myListModel.addListener(listener);
    myListModel.addRemovedElementListener(listener);
  }

  public Object getRoot() {
    return myOriginalTreeModel.getRoot();
  }

  public Object getChild(Object parent, int index) {
    int parentIndex = indexOf(parent);
    if (parentIndex == -1)
      return getOriginalChild(parent, index);
    int parentLevel = pathAt(parentIndex).getPathCount() - 1;
    if (parentIndex == myListModel.getSize() - 1)
      return getOriginalChild(parent, index);
    TreePath nextPath = pathAt(parentIndex + 1);
    if (nextPath.getPathCount() <= parentLevel || !Util.equals(parent, nextPath.getPathComponent(parentLevel)))
      return getOriginalChild(parent, index);
    assert parentIndex + index < myListModel.getSize() : "Parent: " + parent + listToString();
    int listIndex = parentIndex + 1;
    for (int toSkip = index; toSkip > 0; toSkip--)
      listIndex = lastDescendantIndex(listIndex) + 1;
    TreePath childPath = pathAt(listIndex);
    assert childPath.getPathCount() - 1 == parentLevel + 1 :
      "Parent: " + parentIndex + " (" + parent + ")" + " index: " + index + listToString();
    return childPath.getLastPathComponent();
  }

  private Object getOriginalChild(Object parent, int index) {
    Object child = myOriginalTreeModel.getChild(parent, index);
    myNotInList.add(child);
    return child;
  }

  public int getChildCount(Object parent) {
    return myOriginalTreeModel.getChildCount(parent);
  }

  public boolean isLeaf(Object node) {
    return myOriginalTreeModel.isLeaf(node);
  }

  public void valueForPathChanged(TreePath path, Object newValue) {
    myOriginalTreeModel.valueForPathChanged(path, newValue);
  }

  public int getIndexOfChild(Object parent, Object child) {
    if (parent == null || child == null)
      return -1;
    int parentIndex = indexOf(parent);
    if (parentIndex == -1)
      return myOriginalTreeModel.getIndexOfChild(parent, child);
    TreePath path = pathAt(parentIndex);
    int pathCount = path.getPathCount();
    for (int i = parentIndex + 1; i < myListModel.getSize(); i++) {
      TreePath descendantPath = pathAt(i);
      int level = descendantPath.getPathCount();
      if (level > pathCount + 1)
        continue;
      if (level <= pathCount)
        return -1;
      if (descendantPath.getLastPathComponent() == child)
        return i - parentIndex - 1;
    }
    return -1;
  }

  public void addTreeModelListener(TreeModelListener l) {
    myListeners.addStraightListener(Lifespan.FOREVER, l);
  }

  public void removeTreeModelListener(TreeModelListener l) {
    myListeners.removeListener(l);
  }

  private int indexOf(Object node) {
    for (int i = 0; i < myListModel.getSize(); i++) {
      TreePath path = pathAt(i);
      if (path.getLastPathComponent() == node)
        return i;
    }
    assert false: "Node: " + node + listToString();
    return -1;
  }

  private int lastDescendantIndex(int parentIndex) {
    TreePath path = pathAt(parentIndex);
    Object parent = path.getLastPathComponent();
    int result = parentIndex + 1;
    while (result < myListModel.getSize() && pathAt(result).getPathComponent(path.getPathCount() - 1) == parent)
      result++;
    return result - 1;
  }

  private int parentIndex(int childIndex) {
    TreePath path = pathAt(childIndex);
    int parentPathLength = path.getPathCount() - 1;
    for (int i = childIndex - 1; i >= 0; i--) {
      int pathCount = pathAt(i).getPathCount();
      if (parentPathLength < pathCount)
        continue;
      assert pathCount == parentPathLength : "Child: " + childIndex + listToString();
      return i;
    }
    assert false : "Child: " + childIndex + listToString();
    return 0;
  }

  private String listToString() {
    return " list: \n" + TextUtil.separateToString(myListModel.toList(), "\n");
  }

  private TreePath pathAt(int index) {
    return myListModel.getAt(index);
  }

  private class ListListener implements AListModel.Listener, AListModel.RemovedElementsListener<TreePath> {
    public void onInsert(int index, int length) {
      if (index == 0) {
        assert false;
        fireStructureChanged(pathAt(0));
        return;
      }
      new TreeSubsetIterator(index, length) {
        protected void onChildrenFound(int parentIndex, int[] indicies, List<Object> children) {
          boolean hasNotInList = myNotInList.removeAll(children);
          if (hasNotInList)
            fireStructureChanged(getPath(parentIndex));
          else {
            TreeModelEvent event =
              new TreeModelEvent(TreePathToTreeModel.this, pathAt(parentIndex), indicies, children.toArray());
            myListeners.getDispatcher().treeNodesInserted(event);
          }
        }
      }.perform();
    }

    public void onBeforeElementsRemoved(AListModel.RemoveNotice<TreePath> notice) {
      final List<TreeModelEvent> events = Collections15.arrayList();
      new TreeSubsetIterator(notice.getFirstIndex(), notice.getLength()) {
        protected void onChildrenFound(int parentIndex, int[] indicies, List<Object> children) {
          events.add(new TreeModelEvent(TreePathToTreeModel.this, pathAt(parentIndex), indicies, children.toArray()));
        }
      }.perform();
      notice.putUserData(EVENTS, events);
    }

    public void onRemove(int index, int length, final AListModel.RemovedEvent event) {
      // todo user data required
      List<TreeModelEvent> events = (List<TreeModelEvent>) event.getUserData(EVENTS);
      for (TreeModelEvent e : events)
        myListeners.getDispatcher().treeNodesRemoved(e);

    }

    public void onListRearranged(final AListModel.AListEvent event) {
      int lowAffectedIndex = event.getLowAffectedIndex();
      int length = event.getAffectedLength();
      if (lowAffectedIndex == 0) {
        lowAffectedIndex = 1;
        length--;
      }
      if (length <= 0)
        return;
      new TreeSubsetIterator(lowAffectedIndex, length) {
        protected void onChildrenFound(int parentIndex, int[] indicies, List<Object> children) {
          for (int index : indicies) {
            fireStructureChanged(pathAt(parentIndex + index));
          }
        }

        protected boolean shouldIgnore(int index) {
          return !event.isAffected(index);
        }
      }.perform();
    }

    public void onItemsUpdated(final AListModel.UpdateEvent event) {
      new TreeSubsetIterator(event.getLowAffectedIndex(), event.getAffectedLength()){
        protected void onChildrenFound(int parentIndex, int[] indicies, List<Object> children) {
          TreeModelEvent event = new TreeModelEvent(TreePathToTreeModel.this, pathAt(parentIndex), indicies, children.toArray());
          myListeners.getDispatcher().treeNodesChanged(event);
        }

        protected boolean shouldIgnore(int index) {
          return !event.isUpdated(index);
        }
      }.perform();
    }

    private void fireStructureChanged(TreePath parentPath) {
      TreeModelEvent event = new TreeModelEvent(TreePathToTreeModel.this, parentPath);
      myListeners.getDispatcher().treeStructureChanged(event);
    }
  }

  private static abstract class BaseTreeSubsetIterator {
    private final int myIndex;
    private final int myLimitIndex;

    protected BaseTreeSubsetIterator(int index, int length) {
      myIndex = index;
      myLimitIndex = index + length;
    }

    public final void perform() {
      int childPathLength = getPath(getParentIndex(myIndex)).getPathCount() + 1;
      int i = myIndex;
      List<Object> children = Collections15.arrayList();
      while (i < myLimitIndex) {
//        TreePath parentPath = null;
        int parentIndex = -1;
        for (; i < myLimitIndex; i++) {
          TreePath path = getPath(i);
          if (path.getPathCount() > childPathLength || shouldIgnore(i))
            continue;
          if (path.getPathCount() < childPathLength)
            break;
          if (parentIndex == -1)
            parentIndex = getParentIndex(i);
//          if (parentPath == null)
//            parentPath = myListModel.getAt(parentIndex(i));
          children.add(path.getLastPathComponent());
        }
        if (children.size() > 0) {
          int[] indicies = new int[children.size()];
          for (int j = 0; j < indicies.length; j++)
            indicies[j] = j;
          onChildrenFound(parentIndex, indicies, children);
          children.clear();
        }
        childPathLength--;
      }
    }

    protected abstract boolean shouldIgnore(int index);

    protected abstract TreePath getPath(int index);

    protected abstract void onChildrenFound(int parentIndex, int[] indicies, List<Object> children);

    protected abstract int getParentIndex(int childIndex);
  }

  private abstract class TreeSubsetIterator extends BaseTreeSubsetIterator {
    protected TreeSubsetIterator(int index, int length) {
      super(index, length);
    }

    protected boolean shouldIgnore(int index) {
      return false;
    }

    protected TreePath getPath(int index) {
      return pathAt(index);
    }

    protected int getParentIndex(int childIndex) {
      return parentIndex(childIndex);
    }
  }
}
