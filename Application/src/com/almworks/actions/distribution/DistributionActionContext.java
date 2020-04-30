package com.almworks.actions.distribution;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.tree.DistributionFolderNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.ComponentContext;

class DistributionActionContext {
  private final GenericNode myNavigationNode;
  private final TreeNodeFactory myTreeNodeFactory;
  private final NameResolver myNameResolver;
  private final DialogManager myDialogManager;
  private final ATree<?> myTree;
  private final boolean myEdit;
  private final boolean myCreate;

  public DistributionActionContext(ActionContext context) throws CantPerformException {
    ComponentContext<ATree> cc = context.getComponentContext(ATree.class, GenericNode.NAVIGATION_NODE);
    myTree = cc.getComponent();
    myNavigationNode = cc.getSourceObject(GenericNode.NAVIGATION_NODE);
    myTreeNodeFactory = context.getSourceObject(TreeNodeFactory.TREE_NODE_FACTORY);
    myNameResolver = context.getSourceObject(NameResolver.ROLE);
    myDialogManager = context.getSourceObject(DialogManager.ROLE);
    myEdit = myNavigationNode instanceof DistributionFolderNode;
    myCreate = !myEdit && myNavigationNode.allowsChildren(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
  }

  public boolean isEdit() {
    return myEdit;
  }

  public boolean isCreate() {
    return myCreate;
  }

  public GenericNode getNavigationNode() {
    return myNavigationNode;
  }

  public TreeNodeFactory getTreeNodeFactory() {
    return myTreeNodeFactory;
  }

  public NameResolver getNameResolver() {
    return myNameResolver;
  }

  public DialogManager getDialogManager() {
    return myDialogManager;
  }

  public void expandTreeNode(ATreeNode modelBridge) {
    myTree.expand(modelBridge, 1500);
  }

}
