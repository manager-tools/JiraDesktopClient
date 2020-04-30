package com.almworks.util.components;

import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
class TreeSelectionAccessor <T extends ATreeNode> extends SelectionAccessor<T> implements TreeSelectionListener {
  private final JTree myTree;
  private final TreeModelListener myTreeModelListener = new TreeModelAdapter() {
    public void treeNodesChanged(TreeModelEvent e) {
      TreePath path = e.getTreePath();
      Object[] childrenArray = e.getChildren();
      boolean selectedChanged = false;
      if (childrenArray == null) {
        selectedChanged = isSelected((T) path.getLastPathComponent());
      } else {
        List<Object> children = Arrays.asList(childrenArray);
        if (path == null)
          return;
        int[] indices = getSelectedIndexes();
        for (int index : indices) {
          TreePath selectedPath = myTree.getPathForRow(index);
          if (selectedPath != null) {
            Object node = selectedPath.getLastPathComponent();
            if (children.contains(node) && path.equals(selectedPath.getParentPath())) {
              selectedChanged = true;
              break;
            }
          }
        }
      }
      if (selectedChanged)
        fireSelectedItemsChanged();
    }
  };
  private ATreeNodeManager myNodeManager;
  private int myLastLeadIndex = -1;

  public TreeSelectionAccessor(JTree tree) {
    myTree = tree;
    myTree.addTreeSelectionListener(this);
    PropertyChangeListener modelChangeListener = new PropertyChangeListener() {
      private TreeModel myPrevModel = null;

      public void propertyChange(PropertyChangeEvent evt) {
        if (myPrevModel != null)
          myPrevModel.removeTreeModelListener(myTreeModelListener);
        myPrevModel = myTree.getModel();
        if (myPrevModel != null)
          myPrevModel.addTreeModelListener(myTreeModelListener);
      }
    };
    myTree.addPropertyChangeListener(JTree.TREE_MODEL_PROPERTY, modelChangeListener);
    modelChangeListener.propertyChange(null);
  }

  public void setNodeManager(ATreeNodeManager nodeManager) {
    myNodeManager = nodeManager;
  }

  public ATreeNodeManager getNodeManager() {
    return myNodeManager;
  }

  @Nullable
  public T getSelection() {
    Threads.assertAWTThread();
    int index = getSelectedIndex();
    if (index >= 0 && index < myTree.getRowCount()) {
      TreePath path = myTree.getPathForRow(index);
      return path == null ? null : (T) path.getLastPathComponent();
    } else {
      return null;
    }
  }

  public boolean hasSelection() {
    return myTree.getSelectionPath() != null;
  }

  @NotNull
  public List<T> getSelectedItems() {
    int[] indices = getSelectedIndexes();
    if (indices.length == 0)
      return Collections15.emptyList();
    List<T> list = Collections15.arrayList();
    for (int index : indices) {
      TreePath path = myTree.getPathForRow(index);
      if (path != null) {
        list.add((T) path.getLastPathComponent());
      }
    }
    return list;
  }

  public T getFirstSelectedItem() {
    TreePath path = myTree.getAnchorSelectionPath();
    return path != null && myTree.isPathSelected(path) ? (T) path.getLastPathComponent() : null;
  }

  public T getLastSelectedItem() {
    TreePath path = myTree.getLeadSelectionPath();
    return path != null && myTree.isPathSelected(path) ? (T) path.getLastPathComponent() : null;
  }

  @NotNull
  @Override
  public int[] getSelectedIndexes() {
    int[] selectionRows = myTree.getSelectionRows();
    return selectionRows != null ? selectionRows : Const.EMPTY_INTS;
  }

  public boolean setSelected(T node) {
    node = normalize(node);
    if (node == null)
      return false;
    TreePath pathToRoot = UIUtil.getPathToRoot(node);
    TreePath parent = pathToRoot.getParentPath();
    if (parent != null)
      myTree.expandPath(parent);
    myTree.setSelectionPath(pathToRoot);
    return true;
  }

  @Nullable
  private T normalize(T node) {
    if (node == null)
      return null;
    if (myNodeManager == null)
      return node;
    else
      return (T) myNodeManager.findCorresponding(node);
  }

  protected int getElementCount() {
    return myTree.getRowCount();
  }

  protected T getElement(int index) {
    return (T) myTree.getPathForRow(index).getLastPathComponent();
  }

  public void setSelectedIndex(int index) {
    myTree.setSelectionRow(index);
  }

  public void valueChanged(TreeSelectionEvent e) {
    if (!checkPathsAlive(myTree.getSelectionPaths())) {
      // Have seen this check fail in a benign situation: when several selected nodes are removed, Swing first removes them from the model and then removes them from selection one by one. 
      // It leads to firing many events with selection containing removed items that have not been processed yet.
      // Warning is here to let us know that we might have missed an important event 
      Log.warn("TSA: skipping event " + myTree.getModel().getChildCount(myTree.getModel().getRoot()));
      return;
    }
    fireSelectionChanged();
  }
  
  private boolean checkPathsAlive(TreePath[] paths) {
    if (paths == null)
      return true;
    for (TreePath path : paths) {
      if (!((ATreeNode<?>) path.getLastPathComponent()).isAttachedToModel())
        return false;
    }
    return true;
  }

  public void selectAll() {
    throw new RuntimeException("Not implemented yet");
  }

  public void clearSelection() {
    myTree.clearSelection();
  }

  public void invertSelection() {
    throw new RuntimeException("Not implemented yet");
  }

  public void addSelectedRange(int first, int last) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public final void setSelectedIndexes(int[] indices) {
    Threads.assertAWTThread();
    if (indices == null || indices.length == 0)
      return;
    setSelectedIndex(indices[0]);
    for (int i = 1; i < indices.length; i++)
      addSelectionIndex(indices[i]);
  }

  @Override
  public void removeSelectedRange(int first, int last) {
    throw new RuntimeException("Not implemented yet");
  }

  public int getSelectedIndex() {
    // here
    Threads.assertAWTThread();
    int min = myTree.getMinSelectionRow();
    if (min == -1) {
      // selection is empty
      return -1;
    }
    int index;
    int size = myTree.getRowCount();
    // try lead index. NB: lead item may be not selected!
    index = myTree.getLeadSelectionRow();
    if (index >= 0 && index < size && myTree.isRowSelected(index)) {
      myLastLeadIndex = index;
      return index;
    }
    // try last lead index. may be not selected as well
    index = myLastLeadIndex;
    if (index >= 0 && index < size && myTree.isRowSelected(index)) {
      return index;
    }
    // use min selection by default
    return min < size ? min : -1;
  }

  public boolean ensureSelectionExists() {
    if (hasSelection())
      return true;
    TreeModel model = myTree.getModel();
    Object root = model.getRoot();
    Object selection;
    if (myTree.isRootVisible())
      selection = root;
    else {
      int children = model.getChildCount(root);
      if (children == 0)
        return false;
      selection = model.getChild(root, 0);
      myTree.expandPath(new TreePath(root));
    }
    myTree.setSelectionPath(((ATreeNode<?>) selection).getPathFromRoot());
    return true;
  }

  public void addSelection(T item) {
    item = normalize(item);
    if (item == null)
      return;
    myTree.getSelectionModel().addSelectionPath(item.getPathFromRoot());
  }

  public boolean isAllSelected() {
    throw new RuntimeException("Not implemented yet");
  }

  public boolean isSelected(T item) {
    item = normalize(item);
    return item != null && getSelectedItems().contains(item);
  }

  public boolean isSelectedAt(int index) {
    return myTree.isRowSelected(index);
  }

  public void addSelectionIndex(int index) {
    myTree.addSelectionRow(index);
  }

  public void removeSelectionAt(int index) {
    myTree.removeSelectionRow(index);
  }

  public void removeSelection(T element) {
    element = normalize(element);
    if (element == null)
      return;
    myTree.getSelectionModel().removeSelectionPath(element.getPathFromRoot());
  }
}
