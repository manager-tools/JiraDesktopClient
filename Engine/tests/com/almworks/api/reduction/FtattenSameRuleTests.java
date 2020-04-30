package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import junit.framework.Assert;

/**
 * @author dyoma
 */
public class FtattenSameRuleTests extends ReductionTestCase {
  public void test() {
    FlattenSameRule rule = new FlattenSameRule(CompositeConstraint.OR);
    DeafConstraint c1 = new DeafConstraint("1");
    DeafConstraint c2 = new DeafConstraint("2");
    DeafConstraint c3 = new DeafConstraint("3");
    DeafConstraint c4 = new DeafConstraint("4");
    DeafConstraint c5 = new DeafConstraint("5");
    DeafConstraint c6 = new DeafConstraint("6");
    DeafConstraint c7 = new DeafConstraint("7");
    CompositeConstraint.Simple nestedAnd = CompositeConstraint.Simple.and(c6, c7);
    CompositeConstraint.Simple and = CompositeConstraint.Simple.and(c4, c5, nestedAnd);
    CompositeConstraint.Simple simple =
      CompositeConstraint.Simple.or(CompositeConstraint.Simple.or(c1, c2), c3, and);
    ConstraintTreeNode tree = (ConstraintTreeNode) ConstraintTreeElement.createTree(simple);
    ConstraintTreeElement result = rule.process(tree);
    Assert.assertNotNull(result);
    Assert.assertEquals("(1 v 2 v 3 v (4 ^ 5 ^ (6 ^ 7)))", result.toString());
    Assert.assertNull(rule.process(ConstraintTreeElement.createTree(and)));
  }

  public void testNegated() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    ConstraintTreeNode andN = createAnd();
    addChild(andN, "2");
    addChild(andN, "3");
    andN.negate();
    and.addChild(andN);
    ConstraintTreeNode and2 = createAnd();
    addChild(and2, "4");
    addChild(and2, "5");
    and.addChild(and2);
    ConstraintTreeElement result = new FlattenSameRule(CompositeConstraint.AND).process(and);
    Assert.assertNotNull(result);
    Assert.assertEquals("(1 ^ !(2 ^ 3) ^ 4 ^ 5)", result.toString());
  }

  public void testLong() {
    ConstraintTreeNode root = createAnd();
    ConstraintTreeNode and1 = createAnd();
    addChild(and1, "1");
    addChild(and1, "2");
    ConstraintTreeNode and11 = createAnd();
    addChild(and11, "3");
    addChild(and11, "4");
    and1.addChild(and11);
    root.addChild(and1);
    ConstraintTreeNode and2 = createAnd();
    addChild(root, "5");
    root.addChild(and2);
    addChild(and2, "6");
    addChild(and2, "7");
    ConstraintTreeElement result = new FlattenSameRule(CompositeConstraint.AND).process(root);
    Assert.assertNotNull(result);
    Assert.assertEquals("(1 ^ 2 ^ 3 ^ 4 ^ 5 ^ 6 ^ 7)", result.toString());
  }

}
