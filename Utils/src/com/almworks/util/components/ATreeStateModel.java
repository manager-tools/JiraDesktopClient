package com.almworks.util.components;

import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.util.WeakHashMap;

public class ATreeStateModel<T> {

  private final MyListener myListener = new MyListener();
  private final WeakHashMap<T, Boolean> myExpandState = Collections15.weakMap();
  private final WeakHashMap<T, Boolean> mySelectionState = Collections15.weakMap();
  private ATree<ATreeNode<T>> myTree;

  private DetachComposite myLife = null;

  public ATreeStateModel() {
  }

  public void install(Lifespan life, ATree<ATreeNode<T>> tree) {
    if (myLife != null)
      myLife.detach();
    if (life.isEnded())
      return;
    myLife = new DetachComposite();
    life.add(myLife);

    myTree = tree;
    myTree.addExpansionListener(myLife, myListener);
    myTree.addModelListener(myLife, myListener);

  }


  private class MyListener implements TreeExpansionListener, TreeModelListener {
    public void treeExpanded(TreeExpansionEvent event) {
      process(event, true);
    }

    public void treeCollapsed(TreeExpansionEvent event) {
      process(event, false);
    }

    private void process(TreeExpansionEvent event, boolean expand) {
      ATreeNode node = (ATreeNode) event.getPath().getLastPathComponent();
      T userObject = (T)node.getUserObject();
      myExpandState.put(userObject, expand);
    }

    public void treeNodesChanged(TreeModelEvent e) {

    }

    public void treeNodesInserted(TreeModelEvent e) {

    }

    public void treeNodesRemoved(TreeModelEvent e) {

    }

    public void treeStructureChanged(TreeModelEvent e) {

    }
  }
}
