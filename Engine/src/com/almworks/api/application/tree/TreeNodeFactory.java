package com.almworks.api.application.tree;

import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public interface TreeNodeFactory {
  String KLUDGE_COLLECTIONS_FOLDER = "collectionsFolder";
  String KLUDGE_FAVORITES = "favoritesCollection";
  String ITEM_COLLECTION_KEY = "artifactCollection";

  // kludge
  void selectNode(@NotNull GenericNode node, boolean clearOtherSelection);

  void expandNode(@NotNull GenericNode node);

  boolean isExpanded(@NotNull GenericNode node);

  // kludge
  ATree<ATreeNode<GenericNode>> getTree();

  TagNode createTag(GenericNode parent);

  enum NodeType {
    FAVORITES_TAG,
    COLLECTIONS_FOLDER,
    TAG,
    FOLDER,
    QUERY,
    DISTRIBUTION_FOLDER,
    DISTRIBUTION_QUERY,
    DISTRIBUTION_GROUP,
    LAZY_DISTRIBUTION,
    NOTE,
  }

  DataRole<TreeNodeFactory> TREE_NODE_FACTORY = DataRole.createRole(TreeNodeFactory.class);

  <T extends GenericNode> void createAndEditNode(GenericNode parent, NodeType nodeType, ActionContext context)
    throws CantPerformException;

  RenamableNode createFolder(GenericNode parent);

  GenericNode createGeneralFolder(GenericNode parent, ReadonlyConfiguration formula);

  UserQueryNode createUserQuery(GenericNode parent, ReadonlyConfiguration formula);

  UserQueryNode createUserQuery(GenericNode parent);

  DistributionQueryNode createDistributionQuery(GenericNode parent, ReadonlyConfiguration formula);

  DistributionFolderNode createDistributionFolderNode(GenericNode parent);

  DistributionFolderNode createDistributionFolderNode(GenericNode parent, ReadonlyConfiguration formula);

  GenericNode createLazyDistributionNode(GenericNode parent, ReadonlyConfiguration distConfig);
}
