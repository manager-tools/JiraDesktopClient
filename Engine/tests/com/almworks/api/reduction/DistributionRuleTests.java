package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ConstraintNegation;
import junit.framework.Assert;

/**
 * @author dyoma
 */
public class DistributionRuleTests extends ReductionTestCase {
  private final DistributionRule myRule = new DistributionRule(CompositeConstraint.OR);

  public void testBasic() {
    ConstraintTreeNode or = createOr();
    ConstraintTreeLeaf c1 = addChild(or, "1");
    ConstraintTreeNode and = createAnd();
    or.addChild(and);
    addChild(and, "2");
    addChild(and, "3");
    Assert.assertNull(myRule.process(and));
    Assert.assertNull(myRule.process(c1));
    Assert.assertEquals("(1 v (2 ^ 3))", or.toString());
    ConstraintTreeElement result = myRule.process(or);
    Assert.assertNotNull(result);
    Assert.assertEquals("((1 v 2) ^ (1 v 3))", result.toString());
  }

  public void testSeveralAnds() {
    ConstraintTreeNode or = createOr();
    addChild(or, "1");
    addChild(or, "2");
    ConstraintTreeNode and1 = createAnd();
    or.addChild(and1);
    addChild(and1, "3");
    addChild(and1, "4");
    ConstraintTreeNode and2 = createAnd();
    or.addChild(and2);
    addChild(and2, "5");
    addChild(and2, "6");
    Assert.assertEquals("(1 v 2 v (3 ^ 4) v (5 ^ 6))", or.toString());
    ConstraintTreeElement result = myRule.process(or);
    Assert.assertNotNull(result);
    Assert.assertEquals("(((1 v 2 v 3 v 5) ^ (1 v 2 v 3 v 6)) ^ ((1 v 2 v 4 v 5) ^ (1 v 2 v 4 v 6)))", result.toString());
  }

  public void testLongAnd() {
    ConstraintTreeNode or = createOr();
    addChild(or, "1");
    addChild(or, "2");
    ConstraintTreeNode and = createAnd();
    or.addChild(and);
    addChild(and, "3");
    addChild(and, "4");
    addChild(and, "5");
    addChild(and, "6");
    ConstraintTreeElement result = myRule.process(or);
    Assert.assertNotNull(result);
    Assert.assertEquals("((1 v 2 v 3) ^ (1 v 2 v 4) ^ (1 v 2 v 5) ^ (1 v 2 v 6))", result.toString());
  }

  public void testReduction() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    ConstraintTreeNode and2 = createAnd();
    and2.negate();
    addChild(and2, "2");
    addChild(and2, "3");
    and.addChild(and2);
    Assert.assertNull(new FlattenSameRule(CompositeConstraint.AND).process(and));
    Assert.assertEquals("(1 ^ !(2 ^ 3))", and.toString());
    ConstraintTreeElement result = ReductionUtil.toCnf(and);
    Assert.assertEquals("(1 ^ (!2 v !3))", result.toString());
  }

  public void testReducingEmpty() {
    ConstraintTreeNode and = createAnd();
    ConstraintTreeElement cnf = ReductionUtil.toCnf(and.getCopy());
    Assert.assertSame(Constraint.TRUE, cnf.createConstraint().getType());
    and.addChild(createAnd());
    and.addChild(createOr());
    cnf = ReductionUtil.toCnf(and.getCopy());
    Assert.assertSame(Constraint.TRUE, cnf.createConstraint().getType());
    and.negate();
    cnf = ReductionUtil.toCnf(and.getCopy());
    Assert.assertSame(ConstraintNegation.NEGATION, cnf.createConstraint().getType());
    Assert.assertSame(Constraint.TRUE, ((ConstraintNegation) cnf.createConstraint()).getNegated().getType());
  }

  public void testReducingConstants() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    ConstraintTreeNode or = createOr();
    and.addChild(or);
    or.addChild(ConstraintTreeLeaf.createTrue());
    ConstraintTreeElement cnf = ReductionUtil.toCnf(and.getCopy());
    assertInstanceOf(cnf, ConstraintTreeLeaf.class);
    Assert.assertEquals("1", cnf.toString());
  }

  private void assertInstanceOf(Object object, Class<?> aClass) {
    Assert.assertNotNull(object);
    Assert.assertTrue(object.getClass().getName(), aClass.isInstance(object));
  }
}
