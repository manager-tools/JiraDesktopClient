package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class DistributionVisitor<T> {
  private static List<GenericNode> ourNodeList = Collections15.arrayList();

  public T visit(DistributionFolderNodeImpl folder, @Nullable T ticket) {
    Threads.assertAWTThread();
    List<GenericNode> list = ourNodeList;
    boolean usedStatic = list != null;
    if (usedStatic) {
      ourNodeList = null;
      assert list.size() == 0 : list + " " + this;
    } else {
      // iteration inside iteration
      list = Collections15.arrayList();
    }
    try {
      ticket = collect(folder, true, ticket, list);
      return iterate(list, ticket);
    } finally {
      try {
        if (usedStatic) {
          list.clear();
          ourNodeList = list;
        }
      } catch (Exception e) {
        Log.error(e);
      }
    }
  }

  private T iterate(List<GenericNode> list, T ticket) {
    for (GenericNode child : list) {
      if (child instanceof DistributionQueryNodeImpl) {
        ticket = visitQuery((DistributionQueryNodeImpl) child, ticket);
      } else if (child instanceof DistributionGroupNodeImpl) {
        ticket = visitGroup((DistributionGroupNodeImpl) child, ticket);
      }
    }
    return ticket;
  }

  protected T collectGroup(DistributionGroupNodeImpl group, T ticket, List<GenericNode> list) {
    list.add(group);
    return collect(group, false, ticket, list);
  }

  protected T collectQuery(DistributionQueryNodeImpl query, T ticket, List<GenericNode> list) {
    list.add(query);
    return ticket;
  }

  protected T visitGroup(DistributionGroupNodeImpl group, T ticket) {
    return ticket;
  }

  protected T visitQuery(DistributionQueryNodeImpl query, T ticket) {
    return ticket;
  }

  private T collect(GenericNode parent, boolean visitGroups, T ticket, List<GenericNode> list) {
    TreeModelBridge<GenericNode> treeNode = parent.getTreeNode();
    int count = treeNode.getChildCount();
    for (int i = 0; i < count; i++) {
      GenericNode child = treeNode.getChildAt(i).getUserObject();
      if (child instanceof DistributionQueryNodeImpl) {
        ticket = collectQuery((DistributionQueryNodeImpl) child, ticket, list);
      } else if (visitGroups && child instanceof DistributionGroupNodeImpl) {
        ticket = collectGroup((DistributionGroupNodeImpl) child, ticket, list);
      }
    }
    return ticket;
  }
}
