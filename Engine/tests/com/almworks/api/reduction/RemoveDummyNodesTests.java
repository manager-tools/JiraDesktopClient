package com.almworks.api.reduction;

import junit.framework.Assert;

/**
 * @author dyoma
 */
public class RemoveDummyNodesTests extends ReductionTestCase {
  private final RemoveDummyNodes myRule = new RemoveDummyNodes();

  // Composite constraint with single child
  public void testFlatteningNegated() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    ConstraintTreeNode or = createOr();
    and.addChild(or);
    or.negate();
    addChild(or, "2");
    checkResult("(1 ^ !2)", and);
  }

  // A & true |- A, A & false |- false
  public void testRemoveAndConstant() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    and.addChild(ConstraintTreeLeaf.createTrue());
    checkResult("1", and);
    and = createAnd();
    addChild(and, "1");
    and.addChild(ConstraintTreeLeaf.createFalse());
    checkResult("!true", and);
  }

  // A | false |- A, A | true |- true
  public void testRemoveOrConstant() {
    ConstraintTreeNode or = createOr();
    addChild(or, "1");
    or.addChild(ConstraintTreeLeaf.createFalse());
    checkResult("1", or);
    or = createOr();
    addChild(or, "1");
    or.addChild(ConstraintTreeLeaf.createTrue());
    checkResult("true", or);
  }

  // #848 A & A & A |- A, A | A | A |- A
  public void testSimplifySame() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "1");
    addChild(and, "1");
    checkResult("1", and);

    ConstraintTreeNode or = createOr();
    addChild(or, "1");
    addChild(or, "1");
    checkResult("1", or);
  }

  // #848 A & !A & B |- false, A | !A | B |- true
  public void testSimplifyNegated() {
    ConstraintTreeNode and = createAnd();
    ConstraintTreeLeaf c1 = addChild(and, "1");
    ConstraintTreeLeaf nc1 = c1.getCopy();
    nc1.negate();
    and.addChild(nc1);
    addChild(and, "2");
    checkResult("!true", and);

    ConstraintTreeNode or = createOr();
    or.addChild(c1);
    or.addChild(nc1);
    addChild(or, "2");
    checkResult("true", or);
  }

  // todo #848 X&(A|b)&(A|!b) |- X&A
/*
  public void testComplex() {
    ConstraintTreeNode and = createAnd();
    addChild(and, "X");
    ConstraintTreeNode or1 = createOr();
    ConstraintTreeLeaf A = addChild(or1, "A");
    ConstraintTreeLeaf b = addChild(or1, "b");
    and.addChild(or1);
    ConstraintTreeNode or2 = createOr();
    or2.addChild(A.getCopy());
    ConstraintTreeLeaf bCopy = b.getCopy();
    bCopy.negate();
    or2.addChild(bCopy);
    and.addChild(or2);
    checkResult("X^A", and);
  }
*/

  private void checkResult(String expected, ConstraintTreeNode and) {
    ConstraintTreeElement result = and.getCopy();
    ConstraintTreeElement lastResult = null;
    while (result != null) {
      lastResult = result;
      result = myRule.process(lastResult);
    }
    Assert.assertNotNull(lastResult);
    Assert.assertEquals(expected, lastResult.toString());
  }
}
