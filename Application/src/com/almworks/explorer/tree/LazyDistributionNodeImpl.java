package com.almworks.explorer.tree;

import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.items.api.Database;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeExpansionAdapter;
import com.almworks.util.components.TreeExpansionAware;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeExpansionEvent;

public class LazyDistributionNodeImpl extends GenericNodeImpl
  implements LazyDistributionExpanderNode, TreeExpansionAware
{
  private final TreeExpansionAdapter myExpander = new TreeExpansionAdapter() {
    public void treeExpanded(TreeExpansionEvent event) {
      expand();
    }
  };

  public LazyDistributionNodeImpl(Database db, String name, Configuration config) {
    super(db, new FixedText(name, Icons.NODE_DISTRIBUTION_FOLDER_OPEN, Icons.NODE_DISTRIBUTION_FOLDER_CLOSED), config);
    beRemovable();
    addChildNode(new DistributionFolderNodeImpl.ExpandingProgressNode(db, config));
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return QueryResult.NO_RESULT;
  }

  public boolean isCopiable() {
    return true;
  }

  public void expand() {
    GenericNode parent = getParent();
    if (parent == null)
      return;
    RootNode root = getRoot();
    if (root == null)
      return;
    TreeNodeFactory factory = root.getNodeFactory();
    if (factory == null)
      return;
    ATree<ATreeNode<GenericNode>> tree = factory.getTree();
    boolean wasSelected = tree.getSelectionAccessor().isSelected(this.getTreeNode());
    Configuration config = parent.getConfiguration().createSubset(ConfigNames.KLUDGE_DISTRIBUTION_FOLDER_TAG_NAME);
    ConfigurationUtil.copyTo(getConfiguration(), config);
    DistributionFolderNodeImpl folder = new DistributionFolderNodeImpl(root.getEngine().getDatabase(), getPresentation().getText(), config);
    folder.showExpadingNodeWhenInserted();
    folder.expandAfterNextUpdate();
    int index = removeFromTree();
    parent.getTreeNode().insert(folder.getTreeNode(), index);
    factory.expandNode(folder);
    if (wasSelected) {
      factory.selectNode(folder, false);
    }
  }

  public TreeExpansionAdapter getExpansionEventSink() {
    return myExpander;
  }

  public FixedText getPresentation() {
    return (FixedText) super.getPresentation();
  }

  public ReadonlyConfiguration createCopy(Configuration parentConfig) {
    Configuration copy = (Configuration) super.createCopy(parentConfig);
    Configuration prototype = getConfiguration().getSubset(ConfigNames.PROTOTYPE_TAG);
    if (prototype != null && !prototype.isEmpty()) {
      ConfigurationUtil.copyTo(prototype, copy.createSubset(ConfigNames.PROTOTYPE_TAG));
    }
    return copy;
  }

//  final class LazyDistributionExpanderImpl extends GenericNodeImpl implements LazyDistributionExpanderNode {
//    public LazyDistributionExpanderImpl(Configuration config) {
//      super(new ExpanderPresentation(), config);
//    }
//
//    @NotNull
//    @ThreadSafe
//    public QueryResult getQueryResult() {
//      return QueryResult.NO_RESULT;
//    }
//
//    public boolean isCopiable() {
//      return false;
//    }
//
//    public void expand() {
//      LazyDistributionNodeImpl.this.expand();
//    }
//  }
                                          

//  private final class ExpanderPresentation implements CanvasRenderable {
//    private Color myForeground;
//
//    public void renderOn(Canvas canvas, CellState state) {
//      if (!state.isSelected()) {
//        if (myForeground == null)
//          myForeground = ColorUtil.between(state.getDefaultForeground(), state.getDefaultBackground(), 0.5F);
//        canvas.setForeground(myForeground);
//      }
//      canvas.appendText("Double-click to expand...");
//    }
//  }


//  private static class FolderPresentation extends FixedText {
//    public FolderPresentation(String name) {
//      super(name, Icons.NODE_DISTRIBUTION_FOLDER_OPEN, Icons.NODE_DISTRIBUTION_FOLDER_CLOSED);
//    }
//
//    public void renderOn(Canvas canvas, CellState state) {
//      canvas.appendText("Distribution: ");
//      super.renderOn(canvas, state);
//    }
//  }
}
