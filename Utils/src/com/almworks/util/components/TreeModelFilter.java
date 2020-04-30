package com.almworks.util.components;

import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import java.util.Map;

public class TreeModelFilter<T> implements ATreeNodeManager<T> {
  private final DefaultTreeModel mySourceModel = new DefaultTreeModel(null);
  private final Lifecycle mySourceLife = new Lifecycle();
  private final Map<ATreeNode<T>, TreeFilterNode<T>> myMapping = Collections15.hashMap();
  private final TreeFilterNode<T> myRoot = new TreeFilterNode<T>(this, null, 0, null);

  @Nullable
  private Condition<ATreeNode<T>> myFilter;

  @Nullable
  private DefaultTreeModel myModel;

  public TreeModelFilter() {
  }

  public ATreeNode<T> findCorresponding(ATreeNode<T> subject) {
    if (subject instanceof TreeFilterNode) {
      TreeFilterNode filterNode = ((TreeFilterNode) subject);
      if (filterNode.getNodeManager() == this)
        return subject;
    }
    ATreeNode<T> rootSource = myRoot.getSourceNode();
    if (rootSource == null)
      return null;
    ATreeNode<T> lowerCorresponding = rootSource.getNodeManager().findCorresponding(subject);
    if (lowerCorresponding == null)
      return null;
    if (lowerCorresponding == rootSource)
      return myRoot;
    TreeFilterNode<T> corresponding = getMapped(lowerCorresponding);
    return corresponding;
  }

  public static <T> TreeModelFilter<T> create() {
    return new TreeModelFilter<T>();
  }

  public TreeFilterNode<T> getFilteredRoot() {
    return myRoot;
  }

  public ATreeNode<T> getSourceRoot() {
    return myRoot.getSourceNode();
  }

  public boolean isAttachedToModel() {
    return myModel != null;
  }

  public void setFilter(Condition<ATreeNode<T>> filter) {
    if (!Util.equals(myFilter, filter)) {
      myFilter = filter;
      myRoot.resync(true);
    }
  }

  public void setSourceRoot(ATreeNode<T> fullModel) {
    mySourceLife.cycle();
    Lifespan life = mySourceLife.lifespan();
    new MyListener().attach(life, mySourceModel);
    clear();
    if (fullModel != null) {
      fullModel.replaceRoot(mySourceModel);
      life.add(new Detach() {
        protected void doDetach() {
          mySourceModel.setRoot(null);
        }
      });
    }
  }

  private void clear() {
    myRoot.clear();
    myMapping.clear();
  }

  boolean accept(ATreeNode<T> node) {
    if (myFilter == null)
      return true;
    else
      return myFilter.isAccepted(node);
  }

  TreeFilterNode<T> getMapped(ATreeNode<T> sourceChild) {
    return myMapping.get(sourceChild);
  }

  void notifyModelNodeStructureChanged(TreeFilterNode<T> node) {
    if (myModel != null)
      myModel.nodeStructureChanged(node);
  }

  void notifyModelNodesChanged(TreeFilterNode<T> node, int[] indices) {
    if (myModel != null)
      myModel.nodesChanged(node, indices);
  }

  void notifyModelNodesWereInserted(TreeFilterNode<T> node, int[] indices) {
    if (myModel != null)
      myModel.nodesWereInserted(node, indices);
  }

  void notifyModelNodesWereRemoved(TreeFilterNode<T> node, int[] indices, Object[] children) {
    if (myModel != null)
      myModel.nodesWereRemoved(node, indices, children);
  }

  void putMapping(ATreeNode<T> sourceNode, TreeFilterNode<T> imageNode) {
    myMapping.put(sourceNode, imageNode);
    ATreeNode<T> sourceRoot = myRoot.getSourceNode();
    if (sourceRoot != null) {
      for (int i = 0; i < imageNode.getChildCount(); i++) {
        TreeFilterNode<T> imageChild = imageNode.getChildAt(i);
        ATreeNode<T> sourceChild = imageChild.getSourceNode();
        if (sourceChild != null && sourceChild.getRoot() == sourceRoot) {
          putMapping(sourceChild, imageChild);
        }
      }
    }
  }

  TreeFilterNode<T> removeMapping(ATreeNode<T> sourceNode) {
    TreeFilterNode<T> removed = myMapping.remove(sourceNode);
    for (int i = 0; i < sourceNode.getChildCount(); i++) {
      ATreeNode<T> sourceChild = sourceNode.getChildAt(i);
      TreeFilterNode<T> imageChild = removeMapping(sourceChild);
      assert imageChild == null || imageChild.getParent() == removed;
    }
    return removed;
  }

  void setModel(DefaultTreeModel model) {
    myModel = model;
  }

  public void resyncAll() {
    myRoot.resync(true);
  }


  private class MyListener extends Detach implements TreeModelListener {
    private DefaultTreeModel mySourceModel;
    private boolean myEnded;

    public void preDetach() {
      super.preDetach();
      myEnded = true;
    }

    protected void doDetach() throws Exception {
      DefaultTreeModel sourceModel = mySourceModel;
      if (sourceModel != null) {
        sourceModel.removeTreeModelListener(this);
        mySourceModel = null;
      }
    }

    public void attach(Lifespan life, DefaultTreeModel sourceModel) {
      mySourceModel = sourceModel;
      sourceModel.addTreeModelListener(this);
      life.add(this);
    }

    public void treeNodesChanged(TreeModelEvent e) {
      if (!myEnded) {
        Object[] path = e.getPath();
        TreeFilterNode image = getImage(path);
        if (image != null)
          image.nodesChanged(e.getChildIndices());
      }
    }

    public void treeNodesInserted(TreeModelEvent e) {
      if (!myEnded) {
        Object[] path = e.getPath();
        TreeFilterNode image = getImage(path);
        if (image != null)
          image.nodesInserted(e.getChildIndices());
      }
    }

    public void treeNodesRemoved(TreeModelEvent e) {
      if (!myEnded) {
        Object[] path = e.getPath();
        TreeFilterNode image = getImage(path);
        if (image != null)
          image.nodesRemoved(e.getChildren());
      }
    }

    public void treeStructureChanged(TreeModelEvent e) {
      if (!myEnded) {
        Object[] path = e.getPath();
        TreeFilterNode image = getImage(path);
        if (image != null)
          image.structureChanged(path);
      }
    }

    private TreeFilterNode getImage(Object[] path) {
      int length = path.length;
      assert length > 0;
      if (length == 1)
        return myRoot;
      else
        return myMapping.get(path[length - 1]);
    }
  }
}
