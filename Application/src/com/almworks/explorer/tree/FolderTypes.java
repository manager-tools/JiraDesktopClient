package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.items.api.Database;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
class FolderTypes {
  private static final Map<String, FolderType> ourNodeTypes = Collections15.hashMap();

  static {
    FolderType[] allTypes = new FolderType[] {TreeNodeFactoryImpl.TYPE_FOLDER, TreeNodeFactoryImpl.TYPE_QUERY,
      TreeNodeFactoryImpl.USER_TAG, TreeNodeFactoryImpl.DISTRIBUTION_FOLDER_TYPE,
      TreeNodeFactoryImpl.DISTRIBUTION_QUERY_TYPE, TreeNodeFactoryImpl.DISTRIBUTION_GROUP_TYPE, 
      TreeNodeFactoryImpl.LAZY_DISTRIBUTION_TYPE,
      TreeNodeFactoryImpl.NOTE, TreeNodeFactoryImpl.TAGS_FOLDER_TYPE, TreeNodeFactoryImpl.FAVORITES_TYPE,
    };
    for (FolderType<?> type : allTypes) {
      ourNodeTypes.put(type.getConfigName(), type);
    }
  }

  public static void loadNodes(Database db, GenericNode parent, Configuration config) {
    List<Configuration> nodeConfigs = config.getAllSubsets(null);
    Procedure<GenericNode> insert = parent.createNodeInsert();
    for (Configuration configuration : nodeConfigs) {
      GenericNode node = loadNode(db, configuration);
      if (node != null) insert.invoke(node);
    }
  }

  @Nullable
  public static GenericNode loadNode(Database db, Configuration configuration) {
    FolderType nodeType = getNodeType(configuration);
    return nodeType != null ? nodeType.create(db, configuration) : null;
  }

  public static FolderType getNodeType(ReadonlyConfiguration configuration) {
    String typeName = configuration.getName();
    return ourNodeTypes.get(typeName);
  }
}
