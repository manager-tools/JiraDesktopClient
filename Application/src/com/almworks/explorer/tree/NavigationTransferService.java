package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.items.api.Database;
import com.almworks.util.BadFormatException;
import com.almworks.util.TODO;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.config.*;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.TreeStringTransferService;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author dyoma
 */
class NavigationTransferService implements TreeStringTransferService {
  private static final TypedKey<Map<String, ReadonlyConfiguration>> NAVIGATION_TRANSFER_CACHE_MAP
    = TypedKey.create("NAVIGATION_TRANSFER_CACHE_MAP");
  private final Database myDb;

  public NavigationTransferService(Database db) {
    myDb = db;
  }

  public String exportString(ATreeNode node) {
    GenericNode object = (GenericNode) node.getUserObject();
    return JDOMConfigurator.writeConfiguration(object.createCopy(MapMedium.createConfig()));
  }

  public ATreeNode parseAndCreateNode(String xml, ATreeNode parentNode) throws ParseException {
    GenericNode parent = (GenericNode) parentNode.getUserObject();
    ReadonlyConfiguration childCopy;
    try {
      childCopy = JDOMConfigurator.parse(xml);
    } catch (BadFormatException e) {
      throw new ParseException(e.getMessage(), 0, xml.length(), xml);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), 0, xml.length(), xml);
    }
    Configuration childConfig = parent.getConfiguration().createSubset(childCopy.getName());
    ConfigurationUtil.copyTo(childCopy, childConfig);
    GenericNode node = FolderTypes.loadNode(myDb, childConfig);
    if (node == null)
      throw new ParseException("Wrong xml", 0, xml.length(), xml);
    return node.getTreeNode();
  }

  public boolean isParseable(String string) {
    try {
      FolderType type = FolderTypes.getNodeType(JDOMConfigurator.parse(string));
      return type != null;
    } catch (BadFormatException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean canExport(Collection<ATreeNode> nodes) {
    for (ATreeNode node : nodes) {
      if (!((GenericNode) node.getUserObject()).isCopiable()) {
        return false;
      }
    }
    return true;
  }

  public boolean shouldReplaceOnPaste(ATreeNode oldNode, ATreeNode newNode) {
    return false;
  }

  public boolean shouldFlattenUnder(ATreeNode parent, ATreeNode node) {
    return false;
  }

  public boolean canImportUnder(ATreeNode parent, int insertIndex, String string, DragContext context) {
    GenericNode node = (GenericNode) parent.getUserObject();
    if (string == null)
      return node.allowsAnyChildren();
    FolderType<?> type;
    try {
      ReadonlyConfiguration config = getCachedConfiguration(string, context);
      type = config == Configuration.EMPTY_READONLY_CONFIGURATION ? null : FolderTypes.getNodeType(config);
    } catch (RuntimeException e) {
      type = null; // May happen if string contains valid XML but not valid config XML
    }
    TreeNodeFactory.NodeType nodeType = type != null ? type.getNodeType() : null;
    return nodeType != null && node.allowsChildren(nodeType);
  }

  private ReadonlyConfiguration getCachedConfiguration(String string, DragContext context) {
    Map<String, ReadonlyConfiguration> map = context.getValue(NAVIGATION_TRANSFER_CACHE_MAP);
    if (map == null) {
      map = Collections15.hashMap();
      context.putValue(NAVIGATION_TRANSFER_CACHE_MAP, map);
    }
    ReadonlyConfiguration config = map.get(string);
    if (config == null) {
      try {
        config = JDOMConfigurator.parse(string);
      } catch (BadFormatException e) {
        config = Configuration.EMPTY_READONLY_CONFIGURATION;
      } catch (IOException e) {
        config = Configuration.EMPTY_READONLY_CONFIGURATION;
      }
      map.put(string, config);
    }
    return config;
  }

  public boolean canRemove(ATreeNode node) {
    return ((GenericNode) node.getUserObject()).isRemovable();
  }

  @Nullable
  public ATreeNode createDefaultRoot() {
    return null;
  }

  public void moveNode(ATreeNode child, ATreeNode parent, int index) {
    throw TODO.shouldNotHappen("Shouldn't call");
  }

  public int removeNode(ATreeNode node) {
    return ((GenericNode) node.getUserObject()).removeFromTree();
  }
}
