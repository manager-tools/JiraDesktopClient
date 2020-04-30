package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.ConstraintNegation;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public class ConstraintConvertionTests extends BaseTestCase {
  private final DeafConstraint myC1 = new DeafConstraint("1");
  private final DeafConstraint myC2 = new DeafConstraint("2");

  public void testFromConstraint() {
    ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) ConstraintTreeElement.createTree(myC1);
    assertFalse(leaf.isNegated());
    assertSame(myC1, leaf.getConstraint());
    ConstraintNegation.Simple negated = new ConstraintNegation.Simple(myC1);
    leaf = (ConstraintTreeLeaf) ConstraintTreeElement.createTree(negated);
    assertTrue(leaf.isNegated());
    assertSame(myC1, leaf.getConstraint());
    ConstraintTreeElement and = ConstraintTreeElement.createTree(
      CompositeConstraint.Simple.and(new ConstraintNegation.Simple(myC1), myC2));
    assertEquals("(!1 ^ 2)", and.toString());
  }

  public void testToConstraint() {
    ConstraintTreeLeaf leaf = new ConstraintTreeLeaf(myC1);
    assertSame(myC1, leaf.createConstraint());
    leaf.negate();
    ConstraintNegation negation = (ConstraintNegation) leaf.createConstraint();
    assertSame(ConstraintNegation.NEGATION, negation.getType());
    assertSame(myC1, negation.getNegated());

    ConstraintTreeLeaf leaf2 = new ConstraintTreeLeaf(myC2);
    ConstraintTreeNode node = new ConstraintTreeNode();
    node.addChild(leaf);
    node.addChild(leaf2);
    node.setType(CompositeConstraint.AND);
    CompositeConstraint and = (CompositeConstraint) node.createConstraint();
    assertSame(CompositeConstraint.AND, and.getType());
    assertSame(myC2, and.getChildren().get(1));
    negation = (ConstraintNegation) and.getChildren().get(0);
    assertSame(myC1, negation.getNegated());

    node.negate();
    negation = (ConstraintNegation) node.createConstraint();
    assertSame(ConstraintNegation.NEGATION, negation.getType());
    assertSame(myC2, ((CompositeConstraint) negation.getNegated()).getChildren().get(1));
  }
}
