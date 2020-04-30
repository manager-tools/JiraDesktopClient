package com.almworks.explorer.tree;

import com.almworks.api.application.ItemKeyGroup;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.items.api.Database;
import com.almworks.util.components.EditableText;
import com.almworks.util.components.TreeEvent;
import com.almworks.util.components.TreeListener;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DistributionGroupNodeImpl extends GenericNodeImpl implements DistributionGroupNode {
  private static final String INVALID_GROUP = "invalid group";

  private final ParentResult myResult = new ParentResult(this);

  private String myName;
  private ItemKeyGroup myGroup;
  private final HidingEmptyChildren myHidingChildren = new HidingEmptyChildren(this);

  public DistributionGroupNodeImpl(Database db, Configuration config) {
    super(db, new EditableText("", Icons.NODE_DISTRIBUTION_GROUP), config);
    setPresentation(new HidingPresentation(
        "", Icons.NODE_DISTRIBUTION_GROUP, Icons.NODE_DISTRIBUTION_GROUP_HIDING, this));

    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_QUERY);
    setName(config.getSetting(ConfigNames.NAME_SETTING, INVALID_GROUP));
    getTreeNode().getTreeEventSource().addStraightListener(Lifespan.FOREVER, new TreeListener() {
      public void onTreeEvent(TreeEvent event) {
        if (event.getSource().getParent() == getTreeNode())
          fireTreeNodeChanged();
      }
    });
  }

  private void setName(String name) {
    if (!Util.equals(myName, name)) {
      myName = name;
      ((EditableText) getPresentation()).setText(name);
      fireTreeNodeChanged();
    }
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isCopiable() {
    return false;
  }

  public int compareChildren(GenericNode node1, GenericNode node2) {
    int classDiff = ViewWeightManager.compare(node1, node2);
    if (classDiff != 0) {
      return classDiff;
    }
    if (node1 == null || node2 == null) {
      assert false;
      return 0;
    }
    if ((node1 instanceof DistributionQueryNode) && (node2 instanceof DistributionQueryNode)) {
      DistributionQueryNode q1 = (DistributionQueryNode) node1;
      DistributionQueryNode q2 = (DistributionQueryNode) node2;
      return DistributionFolderNodeImpl.DISTRIBUTION_QUERY_ORDER.compare(q1, q2);
    } else {
      return NavigationTreeUtil.compareNodes(node1, node2);
    }
  }

  public ChildrenOrderPolicy getChildrenOrderPolicy() {
    return ChildrenOrderPolicy.ORDER_ALWAYS;
  }

  public boolean isShowable() {
    GenericNode parent = getParent();
    return parent != null && parent.isShowable();
  }

  public boolean isSameGroup(@NotNull ItemKeyGroup group) {
    if (myGroup != null) {
      return Util.equals(myGroup, group);
    } else {
      return Util.equals(myName, group.getDisplayableName());
    }
  }

  public void updateGroup(ItemKeyGroup group) {
    Threads.assertAWTThread();
    if (!Util.equals(myGroup, group)) {
      myGroup = group;
      String name = group == null ? null : group.getDisplayableName();
      setName(Util.NN(name, INVALID_GROUP));
    }
  }

  @Nullable
  public ItemKeyGroup getGroup() {
    return myGroup;
  }

  public void onInsertToModel() {
    super.onInsertToModel();
    GenericNode parent = getParent();
    if (parent instanceof DistributionFolderNodeImpl) {
//      ((DistributionFolderNodeImpl) parent)
    } else {
      assert false : this + " " + parent;
    }
  }

  @Override
  public void fireTreeNodeChanged() {
    super.fireTreeNodeChanged();
    final GenericNode parent = getParent();
    if(parent instanceof DistributionFolderNodeImpl) {
      parent.fireTreeNodeChanged();
    }
  }

  @Override
  public boolean getHideEmptyChildren() {
    final GenericNode parent = getParent();
    return parent != null ? parent.getHideEmptyChildren() : false;
  }

  public boolean isHidingEmptyChildren() {
    return myHidingChildren.hasHiddenOrNotCounted();
  }
}
