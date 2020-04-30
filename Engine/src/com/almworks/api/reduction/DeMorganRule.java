package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;

/**
 * @author dyoma
 */
public class DeMorganRule implements Rule {
  private final CompositeConstraint.ComplementaryKey myArgumentType;

  public DeMorganRule(CompositeConstraint.ComplementaryKey argument) {
    myArgumentType = argument;
  }

  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeNode node = ConstraintTreeNode.cast(myArgumentType, element);
    if (node == null || !node.isNegated())
      return null;
    assert node.getChildCount() > 0 : node;
    node.setType(myArgumentType.getComplementary());
    node.negate();
    for (ConstraintTreeElement child : node.getChildren()) {
      child.negate();
    }
    return node;
  }
}
