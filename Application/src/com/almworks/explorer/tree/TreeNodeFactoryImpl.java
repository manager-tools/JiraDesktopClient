package com.almworks.explorer.tree;

import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.gui.CommonDialogs;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.items.api.Database;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author dyoma
 */
class TreeNodeFactoryImpl implements TreeNodeFactory {
  public static final String FAVORITES_TAG_NODE_ID = "favorites";

  private final Database myDb;
  private final ATree<ATreeNode<GenericNode>> myTree;

  public TreeNodeFactoryImpl(Database db, ATree<ATreeNode<GenericNode>> tree) {
    myDb = db;
    myTree = tree;
  }

  public RenamableNode createFolder(GenericNode parent) {
    return TYPE_FOLDER.insertNew(myDb, parent);
  }

  public TagNode createTag(GenericNode parent) {
    return USER_TAG.insertNew(myDb, parent);
  }

  // kludge
  public void selectNode(@NotNull GenericNode node, boolean clearOtherSelection) {
    SelectionAccessor<ATreeNode<GenericNode>> accessor = myTree.getSelectionAccessor();
    if (clearOtherSelection)
      accessor.setSelected(node.getTreeNode());
    else
      accessor.addSelection(node.getTreeNode());
    myTree.scrollSelectionToView();
  }

  public void expandNode(@NotNull GenericNode node) {
    myTree.expand(node.getTreeNode());
  }

  public boolean isExpanded(@NotNull GenericNode node) {
    return myTree.isExpanded(node.getTreeNode());
  }

  public UserQueryNode createUserQuery(GenericNode parent) {
    return TYPE_QUERY.insertNew(myDb, parent);
  }

  public NoteNode createNote(GenericNode parent) {
    return NOTE.insertNew(myDb, parent);
  }


  public void createAndEditNode(GenericNode parent, NodeType nodeType, ActionContext context)
    throws CantPerformException
  {
    final SelectionAccessor<ATreeNode<GenericNode>> selection = myTree.getSelectionAccessor();
    final List<ATreeNode<GenericNode>> oldSelection = selection.getSelectedItems();
    DialogEditorBuilder builder;
    final GenericNode newNode;
    switch (nodeType) {
    case FOLDER:
      final RenamableNode folder = createFolder(parent);
      builder = CommonDialogs.createRenameDialog(context, folder.getPresentation(), L.dialog("Create Folder"));
      newNode = folder;
      break;
    case QUERY:
      final UserQueryNodeImpl userQuery = TYPE_QUERY.insertNew(myDb, parent);
      QueryBuilderComponent qb = context.getSourceObject(QueryBuilderComponent.ROLE);
      builder = userQuery.openQueryEditor(L.dialog("Create " + Terms.Query), qb);
      newNode = userQuery;
      break;
    default:
      assert false : nodeType;
      return;
    }
    selection.setSelected(newNode.getTreeNode());
    if (builder != null) {
      builder.addStateListener(new Procedure<DialogEditorBuilder.EditingEvent>() {
        private boolean myWasApplied = false;

        public void invoke(DialogEditorBuilder.EditingEvent arg) {
          myWasApplied |= arg.isApplying();
          if (!myWasApplied) {
            newNode.removeFromTree();
            selection.setSelected(oldSelection);
          }
        }
      });
      builder.showWindow();
    }
  }

  public UserQueryNode createUserQuery(GenericNode parent, ReadonlyConfiguration formula) {
    Configuration configuration = parent.getConfiguration().createSubset(TYPE_QUERY.getConfigName());
    ConfigurationUtil.copyTo(formula, configuration);
    UserQueryNode queryNode = TYPE_QUERY.create(myDb, configuration);
    parent.addChildNode(queryNode);
    return queryNode;
  }

  public GenericNode createGeneralFolder(GenericNode parent, ReadonlyConfiguration formula) {
    Configuration configuration = parent.getConfiguration().createSubset(TYPE_FOLDER.getConfigName());
    ConfigurationUtil.copyTo(formula, configuration);
    GenericNode folderNode = TYPE_FOLDER.create(myDb, configuration);
    parent.addChildNode(folderNode);
    return folderNode;
  }

  public DistributionFolderNode createDistributionFolderNode(GenericNode parent) {
    return DISTRIBUTION_FOLDER_TYPE.insertNew(myDb, parent);
  }

  public DistributionFolderNode createDistributionFolderNode(GenericNode parent, ReadonlyConfiguration formula) {
    Configuration configuration = parent.getConfiguration().createSubset(DISTRIBUTION_FOLDER_TYPE.getConfigName());
    ConfigurationUtil.copyTo(formula, configuration);
    DistributionFolderNodeImpl node = DISTRIBUTION_FOLDER_TYPE.create(myDb, configuration);
    parent.addChildNode(node);
    return node;
  }

  public GenericNode createLazyDistributionNode(GenericNode parent, ReadonlyConfiguration formula) {
    Configuration configuration = parent.getConfiguration().createSubset(LAZY_DISTRIBUTION_TYPE.getConfigName());
    ConfigurationUtil.copyTo(formula, configuration);
    LazyDistributionNodeImpl node = LAZY_DISTRIBUTION_TYPE.create(myDb, configuration);
    parent.addChildNode(node);
    return node;
  }

  public DistributionQueryNode createDistributionQuery(GenericNode parent, ReadonlyConfiguration formula) {
    Configuration configuration = parent.getConfiguration().createSubset(DISTRIBUTION_QUERY_TYPE.getConfigName());
    ConfigurationUtil.copyTo(formula, configuration);
    DistributionQueryNode node = DISTRIBUTION_QUERY_TYPE.create(myDb, configuration);
    parent.addChildNode(node);
    return node;
  }

  static FolderType<FolderNode> TYPE_FOLDER = new FolderNodeType();
  static FolderType<UserQueryNodeImpl> TYPE_QUERY = new QueryFolderType();
  static FolderType<NoteNodeImpl> NOTE = new NoteFolderType();
  static FolderType<TagNodeImpl> USER_TAG = new TagFolderType();
  static FolderType<DistributionFolderNodeImpl> DISTRIBUTION_FOLDER_TYPE = new DistributionFolderType();
  static FolderType<DistributionQueryNodeImpl> DISTRIBUTION_QUERY_TYPE = new DistributionQueryFolderType();
  static FolderType<DistributionGroupNodeImpl> DISTRIBUTION_GROUP_TYPE = new DistributionGroupFolderType();
  static FolderType<LazyDistributionNodeImpl> LAZY_DISTRIBUTION_TYPE = new LazyDistributionFolderType();
  static FolderType<TagsFolderNode> TAGS_FOLDER_TYPE = new TagsFolderType();
  static FolderType<FavoritesNode> FAVORITES_TYPE = new FavoritesFolderType();

  public ATree<ATreeNode<GenericNode>> getTree() {
    return myTree;
  }

  private static class FolderNodeType extends FolderType<FolderNode> {
    public FolderNodeType() {
      super(ConfigNames.FOLDER_KEY, NodeType.FOLDER);
    }

    public FolderNode create(Database db, Configuration configuration) {
      return new FolderNode(db, getName(configuration, L.treeNode("New Folder")), configuration);
    }
  }


  private static class QueryFolderType extends FolderType<UserQueryNodeImpl> {
    public QueryFolderType() {
      super(ConfigNames.USER_QUERY_KEY, NodeType.QUERY);
    }

    public UserQueryNodeImpl create(Database db, Configuration configuration) {
      UserQueryNodeImpl newNode = new UserQueryNodeImpl(db, QueryPresentation.create(configuration), configuration);
      newNode.beRemovable();
      return newNode;
    }
  }


  private static class NoteFolderType extends FolderType<NoteNodeImpl> {
    public NoteFolderType() {
      super(ConfigNames.NOTE_KEY, NodeType.NOTE);
    }

    public NoteNodeImpl create(Database db, Configuration configuration) {
      String name = getName(configuration, L.treeNode("New Note"));
      return new NoteNodeImpl(db, name, configuration);
    }
  }


  private static class TagFolderType extends FolderType<TagNodeImpl> {
    public TagFolderType() {
      super(TreeNodeFactory.ITEM_COLLECTION_KEY, NodeType.TAG);
    }

    public TagNodeImpl create(Database db, Configuration configuration) {
      String name = getName(configuration, L.treeNode("New Tag"));
      String iconPath = configuration.getSetting(ConfigNames.ICON_PATH, null);
      TagNodeImpl newNode = new TagNodeImpl(db, name, iconPath, configuration);
      newNode.beRemovable();
      return newNode;
    }
  }


  private static class DistributionFolderType extends FolderType<DistributionFolderNodeImpl> {
    public DistributionFolderType() {
      super(ConfigNames.KLUDGE_DISTRIBUTION_FOLDER_TAG_NAME, NodeType.DISTRIBUTION_FOLDER);
    }

    public DistributionFolderNodeImpl create(Database db, Configuration configuration) {
      String name = getName(configuration, L.treeNode("Distribution"));
      return new DistributionFolderNodeImpl(db, name, configuration);
    }
  }


  private static class DistributionQueryFolderType extends FolderType<DistributionQueryNodeImpl> {
    public DistributionQueryFolderType() {
      super(ConfigNames.KLUDGE_DISTRIBUTION_QUERY_TAG_NAME, NodeType.DISTRIBUTION_QUERY);
    }

    public DistributionQueryNodeImpl create(Database db, Configuration configuration) {
      String name = getName(configuration, "?");
      return new DistributionQueryNodeImpl(db, name, configuration);
    }
  }


  private static class DistributionGroupFolderType extends FolderType<DistributionGroupNodeImpl> {
    public DistributionGroupFolderType() {
      super(ConfigNames.KLUDGE_DISTRIBUTION_GROUP_TAG_NAME, NodeType.DISTRIBUTION_GROUP);
    }

    public DistributionGroupNodeImpl create(Database db, Configuration configuration) {
      return new DistributionGroupNodeImpl(db, configuration);
    }
  }


  private static class LazyDistributionFolderType extends FolderType<LazyDistributionNodeImpl> {
    public LazyDistributionFolderType() {
      super(ConfigNames.LAZY_DISTRIBUTION_KEY, NodeType.LAZY_DISTRIBUTION);
    }

    public LazyDistributionNodeImpl create(Database db, Configuration configuration) {
      String name = getName(configuration, L.treeNode("Distribution"));
      return new LazyDistributionNodeImpl(db, name, configuration);
    }
  }


  private static class TagsFolderType extends FolderType<TagsFolderNode> {
    public TagsFolderType() {
      super(TreeNodeFactory.KLUDGE_COLLECTIONS_FOLDER, NodeType.COLLECTIONS_FOLDER);
    }

    public TagsFolderNode create(Database db, Configuration configuration) {
      return new TagsFolderNode(db, configuration);
    }
  }


  private static class FavoritesFolderType extends FolderType<FavoritesNode> {
    public FavoritesFolderType() {
      super(TreeNodeFactory.KLUDGE_FAVORITES, NodeType.FAVORITES_TAG);
    }

    public FavoritesNode create(Database db, Configuration configuration) {
      configuration.setSetting(ConfigNames.NODE_ID, FAVORITES_TAG_NODE_ID);
      return new FavoritesNode(db, configuration);
    }
  }
}
