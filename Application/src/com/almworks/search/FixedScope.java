package com.almworks.search;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Collections15;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FixedScope extends SearchScope {
  private final String myName;
  private final Set<String> myNodeIds;
  private List<WeakReference<GenericNode>> myCachedNodes;

  public FixedScope(String name, Collection<String> nodeIds) {
    myName = name;
    myNodeIds = Collections15.hashSet(nodeIds);
  }

  public static FixedScope create(String name, Collection<GenericNode> nodes) {
    List<String> list = Collections15.arrayList(nodes.size());
    for (GenericNode node : nodes) {
      list.add(node.getNodeId());
    }
    return new FixedScope(name, list);
  }

  public String getName() {
    return myName;
  }

  public Collection<GenericNode> getCurrentScope(UpdateContext context) throws CantPerformException {
    if (myCachedNodes == null) {
      ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
      RootNode root = explorer.getRootNode();
      if (root == null)
        return Collections15.emptyCollection();
      List<GenericNode> nodes = root.collectNodes(new Condition<GenericNode>() {
        public boolean isAccepted(GenericNode node) {
          return myNodeIds.contains(node.getNodeId());
        }
      });
      myCachedNodes = Collections15.arrayList(nodes.size());
      for (GenericNode node : nodes) {
        myCachedNodes.add(new WeakReference<GenericNode>(node));
      }
    }
    assert myCachedNodes != null;
    Set<GenericNode> result = Collections15.linkedHashSet();
    for (Iterator<WeakReference<GenericNode>> ii = myCachedNodes.iterator(); ii.hasNext();) {
      WeakReference<GenericNode> ref = ii.next();
      GenericNode node = ref.get();
      if (node == null || !node.isNode()) {
        ii.remove();
        // node is removed
      }
      result.add(node);
    }
    return result;
  }

  public List<String> getNodeIds() {
    if (myCachedNodes != null) {
      List<String> result = Collections15.arrayList();
      for (WeakReference<GenericNode> ref : myCachedNodes) {
        GenericNode node = ref.get();
        if (node != null && node.isNode()) {
          result.add(node.getNodeId());
        }
      }
      return result;
    } else {
      return Collections15.arrayList(myNodeIds);
    }
  }
}
