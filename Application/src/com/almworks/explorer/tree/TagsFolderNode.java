package com.almworks.explorer.tree;

import com.almworks.api.application.tree.*;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.Database;
import com.almworks.items.api.WriteTransaction;
import com.almworks.tags.TagsComponentImpl;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author dyoma
 */
public class TagsFolderNode extends GenericNodeImpl {
  private final ParentResult myResult = new ParentResult(this);
  private final Database myDb;

  public TagsFolderNode(Database db, Configuration config) {
    super(db, new FixedText("Tags", Icons.NODE_SYSTEM_FOLDER_OPEN, Icons.NODE_SYSTEM_FOLDER_CLOSED), config);
    myDb = db;
    addAllowedChildType(TreeNodeFactory.NodeType.TAG);
    addAllowedChildType(TreeNodeFactory.NodeType.FAVORITES_TAG);

    // create favorites
    createFixedTagNode(db, TreeNodeFactory.KLUDGE_FAVORITES, TreeNodeFactoryImpl.FAVORITES_TYPE, FavoritesNode.class, 0);
  }

  private TagNode createFixedTagNode(Database db, String subsetName, FolderType<? extends TagNode> folderType,
    Class<? extends TagNode> nodeClass, int insertIndex)
  {
    TagNode folder = null;
    for (int i = 0; i < getChildrenCount(); i++) {
      GenericNode child = getChildAt(i);
      if (nodeClass.isInstance(child)) {
        folder = (TagNode) child;
      }
    }
    if (folder == null) {
      Configuration subconfig = getConfiguration().createSubset(subsetName);
      folder = folderType.create(db, subconfig);
      TreeModelBridge<GenericNode> node = getTreeNode();
      node.insert(folder.getTreeNode(), Math.min(insertIndex, node.getChildCount()));
    }
    return folder;
  }

  public void onInsertToModel() {
    super.onInsertToModel();
    RootNode root = getRoot();
    if (root != null) {
      root.getNodeFactory().expandNode(this);
    }

    deleteTagsNotHavingNode();
  }

  public void onChildrenChanged() {
    super.onChildrenChanged();
    GenericNode parent = getParent();
    if (parent instanceof RootNodeImpl) {
      ((RootNodeImpl) parent).invalidateTagCache();
    }
  }


  private void deleteTagsNotHavingNode() {
    final Set<String> treeIds = Collections15.hashSet();
    for (int i = 0; i < getChildrenCount(); i++)
      treeIds.add(TagQueryResult.getTagId(getChildAt(i)));

    final TagsComponentImpl tags = Context.require(TagsComponentImpl.class);
    myDb.writeForeground(new WriteTransaction<Object>() {
      public Object transaction(DBWriter writer) {
        LongArray existing = tags.getAllTags(writer);
        if (existing == null) {
          assert false : this;
          return null;
        }
        for (int i = 0; i < existing.size(); i++) {
          long tag = existing.get(i);
          String id = DBAttribute.ID.getValue(tag, writer);
          if (!treeIds.contains(id)) {
            TagsComponentImpl.deleteTag(writer, tag);
          }
        }
        return null;
      }
    });
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isCopiable() {
    return false;
  }
}
