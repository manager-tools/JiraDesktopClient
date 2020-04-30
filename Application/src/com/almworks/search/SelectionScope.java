package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class SelectionScope extends SearchScope {
  public Collection<GenericNode> getCurrentScope(UpdateContext context) throws CantPerformException {
    context.watchRole(GenericNode.NAVIGATION_NODE);
    Collection<GenericNode> nodes;
    try {
      nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    } catch(CantPerformException cpe) {
      nodes = Collections15.emptyList();
    }
    if (nodes.size() == 0) {
      ExplorerComponent explorerComponent = context.getSourceObject(ExplorerComponent.ROLE);
      RootNode rootNode = explorerComponent.getRootNode();
      return Collections.<GenericNode>singletonList(rootNode);
    }
    for (GenericNode node : nodes) {
      if (!node.isNarrowing()) {
        Set<GenericNode> fixup = Collections15.linkedHashSet();
        for (GenericNode n : nodes) {
          while (!n.isNarrowing()) {
            GenericNode parent = n.getParent();
            if (parent == null)
              break;
            n = parent;
          }
          fixup.add(n);
        }
        nodes = fixup;
        break;
      }
    }
    return nodes;
  }

  public String getName() {
    return "Search within selected nodes";
  }
}
