package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public abstract class ReductionTestCase extends BaseTestCase {
  protected ConstraintTreeNode createOr() {
    return create(CompositeConstraint.OR);
  }

  protected ConstraintTreeNode createAnd() {
    return create(CompositeConstraint.AND);
  }

  private ConstraintTreeNode create(CompositeConstraint.ComplementaryKey type) {
    ConstraintTreeNode result = new ConstraintTreeNode();
    result.setType(type);
    return result;
  }

  protected ConstraintTreeLeaf addChild(ConstraintTreeNode node, String label) {
    ConstraintTreeLeaf leaf = new ConstraintTreeLeaf(new DeafConstraint(label));
    node.addChild(leaf);
    return leaf;
  }
}
