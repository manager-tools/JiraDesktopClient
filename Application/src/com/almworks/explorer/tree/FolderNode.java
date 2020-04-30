package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.RenamableNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.items.api.Database;
import com.almworks.util.components.EditableText;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;

import javax.swing.*;

/**
 * @author dyoma
 */
class FolderNode extends GenericNodeImpl implements RenamableNode {
  private final ParentResult myResult = new ParentResult(this);

  public FolderNode(Database db, String folderName, Configuration config) {
    this(db, folderName, config, Icons.NODE_FOLDER_OPEN, Icons.NODE_FOLDER_CLOSED);
  }

  public FolderNode(Database db, String folderName, Configuration config, Icon openIcon, Icon closedIcon) {
    super(db, EditableText.folder(folderName, openIcon, closedIcon), config);
    addAllowedChildType(TreeNodeFactory.NodeType.FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.LAZY_DISTRIBUTION);
    beRemovable();
  }

  public EditableText getPresentation() {
    return (EditableText) super.getPresentation();
  }

  public boolean isCopiable() {
    return areAllChildrenCopiable();
  }

  public QueryResult getQueryResult() {
    return myResult;
  }

  public int compareTo(GenericNode genericNode) {
    int i = super.compareTo(genericNode);
    if (i != 0)
      return i;
    return String.valueOf(this).compareToIgnoreCase(String.valueOf(genericNode));
  }

  public boolean isRenamable() {
    return true;
  }

/*
  public String toString() {
    return getPresentation().getText();
  }
*/

}
