package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Vasya
 */
public class DistributionRule implements Rule {
  private final CompositeConstraint.ComplementaryKey myArgumentType;

  public DistributionRule(CompositeConstraint.ComplementaryKey argumentType) {
    myArgumentType = argumentType;
  }

  @Nullable
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeNode node = ConstraintTreeNode.cast(myArgumentType, element);
    if (node == null)
      return null;
    assert !node.isNegated() : node;
    List<ConstraintTreeElement> children = node.copyChildren();
    assert children.size() > 1 : children.size();
    ConstraintTreeNode complementary = findComplementary(children);
    if (complementary == null)
      return null;
    boolean removed = children.remove(complementary);
    assert removed;
    ConstraintTreeNode result = new ConstraintTreeNode();
    result.setType(myArgumentType.getComplementary());
    for (ConstraintTreeElement complementaryChild : complementary.getChildren()) {
      ConstraintTreeNode composite = new ConstraintTreeNode();
      composite.setType(myArgumentType);
      result.addChild(composite);
      for (ConstraintTreeElement child : children)
        composite.addChild(child.getCopy());
      composite.addChild(complementaryChild.getCopy());
    }
    ReductionUtil.applyToChildren(result, this);
    return result;
  }

  @Nullable
  private ConstraintTreeNode findComplementary(List<ConstraintTreeElement> elements) {
    for (ConstraintTreeElement element : elements) {
      ConstraintTreeNode node = ConstraintTreeNode.cast(myArgumentType.getComplementary(), element);
      if (node != null) {
        assert node.getChildCount() > 1 : node;
        return node;
      }
    }
    return null;
  }
}
