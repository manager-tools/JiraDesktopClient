package com.almworks.util.ui;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeModelAdapter;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.UtilConfigNames;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.List;

/**
 * @author : Dyoma
 */
public class TreeState {
  private final ATree myTree;
  private final Configuration myConfig;
  private final BottleneckJobs<TreePath> myUpdater = new BottleneckJobs<TreePath>(10, ThreadGate.AWT) {
    protected void execute(TreePath job) {
      updateStateFrom(job);
    }
  };

  public TreeState(ATree tree, Configuration config) {
    myTree = tree;
    myConfig = config;
    DefaultTreeModel model = myTree.getModel();
    model.addTreeModelListener(new TreeModelAdapter() {
      public void treeNodesInserted(TreeModelEvent e) {
        myUpdater.addJobDelayed(e.getTreePath());
      }

      public void treeNodesRemoved(TreeModelEvent e) {
        TreePath path = e.getTreePath();
        myUpdater.addJobDelayed(path);
        removeStateFrom(path, e.getChildren());
      }

      public void treeStructureChanged(TreeModelEvent e) {
        assert false;
      }
    });
    ATreeNode root = (ATreeNode) model.getRoot();
    updateStateFrom(root.getPathFromRoot());
    saveState(root);
    myTree.addExpansionListener(new TreeExpansionListener() {
      public void treeCollapsed(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        ATreeNode node = (ATreeNode) path.getLastPathComponent();
        if (node.getChildCount() != 0)
          saveExpansion(path, false);
      }

      public void treeExpanded(TreeExpansionEvent event) {
        saveExpansion(event.getPath(), true);
      }
    });
    myTree.getSelectionAccessor().addAWTChangeListener(Lifespan.FOREVER, new AListModel.Adapter() {
      public void onChange() {
        final List selectedItems = myTree.getSelectionAccessor().getSelectedItems();
        final Configuration subset = myConfig.getOrCreateSubset(UtilConfigNames.SELECTED);
        subset.clear();
        for (int i = 0; i < selectedItems.size(); i++) {
          final String nodeId = getNodeId(selectedItems.get(i));
          if (nodeId != null)
            subset.setSetting(nodeId, true);
        }
      }
    });
  }

  public void updateExpansionNow() {
    myUpdater.executeJobsNow();
  }

  protected void updateStateFrom(TreePath path) {
    Configuration config = findConfig(path, false);
    ATreeNode node = (ATreeNode) path.getLastPathComponent();
    updateStateFrom(node, config, null);
  }

  /**
   * returns buffer
   */
  private List<ATreeNode> updateStateFrom(ATreeNode node, Configuration config, @Nullable List<ATreeNode> buffer) {
    if (config != null && config.getBooleanSetting(UtilConfigNames.EXPANDED, false))
      myTree.expand(node);

    final String nodeId = getNodeId(node);
    if (nodeId != null && myConfig.getOrCreateSubset(UtilConfigNames.SELECTED).getBooleanSetting(nodeId, false))
      myTree.getSelectionAccessor().addSelection(node);

    if (node.getChildCount() > 0) {
      if (buffer == null) {
        buffer = Collections15.arrayList();
      } else {
        buffer.clear();
      }
      node.childrenToList(buffer);
      List<ATreeNode> childBuffer = null;
      for (ATreeNode child : buffer) {
        Configuration subconfig = getSubconfig(config, child, false);
        childBuffer = updateStateFrom(child, subconfig, childBuffer);
      }
      buffer.clear();
    }

    return buffer;
  }

  private void removeStateFrom(TreePath path, Object[] children) {
    for (int i = 0; i < children.length; i++) {
      ATreeNode child = (ATreeNode) children[i];
      Configuration config = findConfig(path.pathByAddingChild(child), false);
      if (config != null)
        config.removeMe();
    }
  }

  private void saveState(ATreeNode node) {
    if (node.getChildCount() == 0)
      return;
    boolean expanded = myTree.isExpanded(node);
    saveExpansion(node.getPathFromRoot(), expanded);
    if (!expanded)
      return;
    Enumeration<ATreeNode> children = node.children();
    while (children.hasMoreElements()) {
      ATreeNode child = children.nextElement();
      saveState(child);
    }
  }

  private void saveExpansion(TreePath path, boolean expanded) {
    myUpdater.removeJob(path);
    Configuration config = findConfig(path, expanded);
    if (config == null)
      return;
    if (!expanded)
      config.removeMe();
    else
      config.setSetting(UtilConfigNames.EXPANDED, true);
  }

  private Configuration findConfig(TreePath path, boolean create) {
    Configuration config = myConfig;
    for (int i = 0; i < path.getPathCount(); i++) {
      config = getSubconfig(config, (ATreeNode) path.getPathComponent(i), create);
      if (config == null)
        return null;
    }
    return config;
  }

  private Configuration getSubconfig(Configuration config, ATreeNode node, boolean create) {
    String nodeId = getNodeId(node);
    if (nodeId == null || config == null)
      return null;
    if (create)
      return config.getOrCreateSubset(nodeId);
    else if (config.isSet(nodeId))
      return config.getSubset(nodeId);
    else
      return null;
  }

  private static String getNodeId(Object nodeBridge) {
    if (!(nodeBridge instanceof ATreeNode))
      return null;
    Object object = ((ATreeNode) nodeBridge).getUserObject();
    if (!(object instanceof IdentifiableNode))
      return null;
    return ((IdentifiableNode) object).getNodeId();
  }

  public void expand(ATreeNode child) {
    myTree.expand(child);
    saveExpansion(child.getPathFromRoot(), true);
  }
}
