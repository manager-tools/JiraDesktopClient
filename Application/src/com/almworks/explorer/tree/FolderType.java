package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.api.config.ConfigNames;
import com.almworks.items.api.Database;
import com.almworks.util.config.Configuration;

/**
 * @author : Dyoma
 */
abstract class FolderType<N extends GenericNode> {
  private final String myConfigName;
  private final TreeNodeFactory.NodeType myNodeType;

  protected FolderType(String configName, TreeNodeFactory.NodeType nodeType) {
    myConfigName = configName;
    myNodeType = nodeType;
  }

  public abstract N create(Database db, Configuration configuration);

  public N insertNew(Database db, GenericNode parent) {
    Configuration folderConfig = parent.getConfiguration().createSubset(myConfigName);
    N newNode = create(db, folderConfig);
    parent.addChildNode(newNode);
    return newNode;
  }

  public String getConfigName() {
    return myConfigName;
  }

  /**
   * @see GenericNode#allowsChildren(com.almworks.api.application.tree.TreeNodeFactory.NodeType)
   */
  public TreeNodeFactory.NodeType getNodeType() {
    return myNodeType;
  }

  protected static String getName(Configuration configuration, String defaultName) {
    return configuration.getSetting(ConfigNames.NAME_SETTING, defaultName);
  }
}
