package com.almworks.explorer.tree;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.tree.DistributionQueryNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.explorer.util.ItemKeys;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

class DistributionComparator implements Comparator<DistributionQueryNode> {
  @Nullable
  private final ItemHypercube myCube;

  public DistributionComparator(@Nullable ItemHypercube cube) {
    myCube = cube;
  }

  public DistributionComparator() {
    this(null);
  }

  public int compare(DistributionQueryNode o1, DistributionQueryNode o2) {
    if (o1 == o2)
      return 0;
    boolean pinned1 = o1.isPinned();
    boolean pinned2 = o2.isPinned();
    if (pinned1 && !pinned2)
      return -1;
    if (pinned2 && !pinned1)
      return 1;
    if (!pinned1 && !pinned2)
      return DistributionFolderNodeImpl.comparePresentations(o1, o2);

    // compare artifact keys
    Pair<ConstraintDescriptor, ItemKey> v1 = o1.getAttributeValue();
    Pair<ConstraintDescriptor, ItemKey> v2 = o2.getAttributeValue();
    if (v1 == null || v2 == null) {
      assert false : o1 + " " + o2;
      return 0;
    }
    if (!v1.getFirst().getId().equals(v2.getFirst().getId())) {
      assert false : v1 + " " + v2;
      return DistributionFolderNodeImpl.comparePresentations(o1, o2);
    }
    ConstraintType type = v1.getFirst().getType();
    if (!(type instanceof EnumConstraintType)) {
//        assert false : type;
      return DistributionFolderNodeImpl.comparePresentations(o1, o2);
    }
    EnumConstraintType enumType = ((EnumConstraintType) type);
    ItemHypercube cube = getCube(o1, o2);
    if (enumType.isNotSetItem(v1.getSecond()))
      return -1;
    if (enumType.isNotSetItem(v2.getSecond()))
      return 1;
    ItemOrder order1 = ItemKeys.getGroupOrder(enumType.resolveKey(v1.getSecond().getId(), cube));
    ItemOrder order2 = ItemKeys.getGroupOrder(enumType.resolveKey(v2.getSecond().getId(), cube));
    return order1.compareTo(order2);
  }

  private ItemHypercube getCube(DistributionQueryNode o1, DistributionQueryNode o2) {
    if (myCube != null) return myCube;
    GenericNode parent1 = o1.getParent();
    GenericNode parent2 = o2.getParent();
    GenericNode commonParent = parent1 == null ? parent2 : parent1;
    assert commonParent != null : o1 + " " + o2;
    assert parent1 == null || parent2 == null || parent1 == parent2 : parent1 + " " + parent2;
    return commonParent == null ? new ItemHypercubeImpl() : commonParent.getHypercube(false);
  }
}
