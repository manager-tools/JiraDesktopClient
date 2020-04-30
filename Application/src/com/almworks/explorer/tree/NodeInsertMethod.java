package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.config.ConfigNames;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.IdentifiableNode;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.*;

public class NodeInsertMethod implements Procedure<GenericNode> {
  private static final Comparator<Pair<String, Integer>> NODE_ORDER_COMPARATOR =
    new Comparator<Pair<String, Integer>>() {
      public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
        return p1.getSecond().compareTo(p2.getSecond());
      }
    };

  private final GenericNodeImpl myParent;
  private Map<String, GenericNode> myChildById = null;
  private int myExpectedChildCount = 0;
  private NodeOrderInfo myOrderInfo = null;

  public NodeInsertMethod(GenericNodeImpl parent) {
    myParent = parent;
  }

  @Override
  public void invoke(GenericNode child) {
    ATreeNode<GenericNode> childNode = child.getTreeNode();
    assert !childNode.isAttachedToModel() : child;
    //noinspection ConstantConditions
    assert childNode.getParent() == null : child + " parent: " + childNode.getParent().getUserObject();
    TreeModelBridge<GenericNode> treeNode = myParent.getTreeNode();
    if (!child.isOrdered()) {
      treeNode.add(childNode);
      updateAfterInsert(child);
      return;
    }
    int insertIndex = getInsertionIndexFromConfig(child.getPositionId());
    treeNode.insert(childNode, insertIndex);
    updateAfterInsert(child);
  }

  private int getInsertionIndexFromConfig(String childId) {
    //noinspection ConstantConditions
    assert childId != null : "childId is null";

    NodeOrderInfo order = myOrderInfo;
    if (order == null) {
      order = NodeOrderInfo.load(myParent.getConfiguration());
      myOrderInfo = order;
    }
    int place = order.getStoredPlace(childId);
    if (place >= 0) {
      int r = restoreIndex(place, childId, order);
      if (r >= 0)
        return r;
    }
    return getDefaultPlace();
  }

  private int getDefaultPlace() {
    int r = 0;
    TreeModelBridge<GenericNode> treeNode = myParent.getTreeNode();
    for (int i = treeNode.getChildCount() - 1; i >= 0; i--) {
      ATreeNode<GenericNode> bridge = treeNode.getChildAt(i);
      assert bridge != null;
      GenericNode node = bridge.getUserObject();
      if (node.isOrdered()) {
        r = i + 1;
        break;
      }
    }
    return r;
  }

  private int restoreIndex(int place, String childId, NodeOrderInfo order) {
    int r = -1;
    Map<String, ? extends GenericNode> childrenById = getChildByIdMap();
    if (childrenById.containsKey(childId)) {
//      assert  false : childId;
      Log.warn("cannot place node " + childId);
      return -1;
    }
    TreeModelBridge<GenericNode> treeNode = myParent.getTreeNode();
    for (int i = 0; i < order.getRecordCount(); i++) {
      String nodeId = order.getNodeId(i);
      GenericNode nextNode = childrenById.get(nodeId);
      if (nextNode != null) {
        int nodeOrder = order.getNodeOrder(i);
        if (nodeOrder > place) {
          int result = treeNode.getIndex(nextNode.getTreeNode());
          assert result >= 0 : result + " " + childId;
          r = result >= 0 ? result : 0;
          break;
        }
      }
    }
    return r;
  }

  private Map<String, ? extends GenericNode> getChildByIdMap() {
    if (myChildById != null && myExpectedChildCount != myParent.getChildrenCount())
      myChildById = null;
    if (myChildById == null) {
      myChildById = (Map<String, GenericNode>) IdentifiableNode.GET_NODE_ID.assignKeys(myParent.getChildren());
      myExpectedChildCount = myParent.getChildrenCount();
    }
    return myChildById;
  }

  private void updateAfterInsert(GenericNode child) {
    if (myChildById == null) return;
    if (myExpectedChildCount != myParent.getChildrenCount() - 1) {
      myChildById = null;
      return;
    }
    String id = IdentifiableNode.GET_NODE_ID.convert(child);
    myChildById.put(id, child);
    myExpectedChildCount++;
  }

  private static enum NodeOrderFormat {
    FORMAT1,
    FORMAT2
  }

  private static class NodeOrderInfo {
    public static final NodeOrderInfo EMPTY = new NodeOrderInfo();
    private final String[] myNodeIds;
    private final int[] myNodeOrders;
    private final int myCount;

    private NodeOrderInfo() {
      this(null, null, 0);
    }

    private NodeOrderInfo(String[] nodeIds, int[] nodeOrders, int count) {
      myNodeIds = nodeIds;
      myNodeOrders = nodeOrders;
      myCount = count;
    }

    public static NodeOrderInfo load(Configuration configuration) {
      Configuration c;
      c = configuration.getSubset(ConfigNames.CHILD_NODE_ORDER_2);
      if (!c.isEmpty()) {
        return load(c, NodeOrderFormat.FORMAT2);
      }
      c = configuration.getSubset(ConfigNames.CHILD_NODE_ORDER);
      if (!c.isEmpty()) {
        return load(c, NodeOrderFormat.FORMAT1);
      }
      return EMPTY;
    }

    private static NodeOrderInfo load(Configuration config, NodeOrderFormat format) {
      Collection<String> names = config.getAllSettingNames();
      int size = names.size();
      String[] nodeIds = new String[size];
      int[] nodeOrders = new int[size];
      SortedSet<Pair<String, Integer>> set = null;
      int lastOrder = Integer.MIN_VALUE;
      boolean straight = true;
      int i = 0;
      for (String name : names) {
        int order = -1;
        String nodeId = null;
        try {
          if (format == NodeOrderFormat.FORMAT2) {
            order = Integer.parseInt(name);
            nodeId = config.getSetting(name, null);
          } else if (format == NodeOrderFormat.FORMAT1) {
            nodeId = name;
            order = config.getIntegerSetting(name, -1);
          } else {
            assert false : format;
          }
          if (nodeId != null && order >= 0) {
            if (straight) {
              if (order >= lastOrder) {
                lastOrder = order;
                nodeIds[i] = nodeId;
                nodeOrders[i] = order;
                i++;
              } else {
                // fallback and create tree
                straight = false;
                set = Collections15.treeSet(NODE_ORDER_COMPARATOR);
                for (int j = 0; j < i; j++) {
                  set.add(Pair.create(nodeIds[j], nodeOrders[j]));
                }
                set.add(Pair.create(nodeId, order));
              }
            } else {
              set.add(Pair.create(nodeId, order));
            }
          }
        } catch (NumberFormatException e) {
          // ignore
        }
      }
      if (!straight) {
        assert set != null;
        size = set.size();
        nodeIds = new String[size];
        nodeOrders = new int[size];
        int k = 0;
        for (Pair<String, Integer> pair : set) {
          nodeIds[k] = pair.getFirst();
          nodeOrders[k] = pair.getSecond();
          k++;
        }
      }
      return new NodeOrderInfo(nodeIds, nodeOrders, i);
    }

    public int getStoredPlace(String nodeId) {
      if (nodeId == null) {
        return -1;
      }
      for (int i = 0; i < myCount; i++) {
        if (nodeId.equals(myNodeIds[i])) {
          return myNodeOrders[i];
        }
      }
      return -1;
    }

    public int getRecordCount() {
      return myCount;
    }

    public String getNodeId(int index) {
      return myNodeIds[index];
    }

    public int getNodeOrder(int index) {
      return myNodeOrders[index];
    }

    public static void clearOrderConfiguration(Configuration configuration) {
      List<Configuration> subsets;
      subsets = configuration.getAllSubsets(ConfigNames.CHILD_NODE_ORDER);
      for (Configuration subset : subsets) {
        subset.removeMe();
      }
      subsets = configuration.getAllSubsets(ConfigNames.CHILD_NODE_ORDER_2);
      for (Configuration subset : subsets) {
        subset.removeMe();
      }
    }
  }
}
